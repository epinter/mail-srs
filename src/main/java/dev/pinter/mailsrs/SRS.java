/*
 * Copyright 2025 Emerson Pinter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.pinter.mailsrs;

import java.util.regex.Pattern;

import static dev.pinter.mailsrs.SRSAddress.Format.SRS0;
import static dev.pinter.mailsrs.SRSAddress.Format.SRS1;

/**
 * The class to rewrite email and SRS addresses.
 */
public class SRS {
    private final String secretKey;
    private final SRSOptions options;
    private static final Pattern PATTERN_SRS0 = Pattern.compile("(?i)^srs0[=\\-+].*");
    private static final Pattern PATTERN_SRS1 = Pattern.compile("(?i)^srs1[=\\-+].*");
    private static final Pattern PATTERN_SRS = Pattern.compile("(?i)^srs[0-1][=\\-+].*");

    /**
     * Builds an SRS instance with default options
     *
     * @param secretKey A secret key used to create the hash
     */
    public SRS(String secretKey) {
        this(secretKey, new SRSOptions.Builder().build());
    }

    /**
     * Builds an SRS instance
     *
     * @param secretKey A secret key used to create the hash, can't be null or empty
     * @param options   Configuration, see {@link SRSOptions}
     */
    public SRS(String secretKey, SRSOptions options) {
        this.secretKey = secretKey;
        this.options = options;
    }

    /**
     * Rewrites an address using Shortcut scheme, which always result in a SRS0 address. only use this if you know
     * what you are doing. The method {@link SRS#forward(String, String)} is recommended, it rewrites an SRS0
     * to SRS1.
     *
     * @param address   Email or srs address
     * @param forwarder The domain of this forwarder
     * @return An SRS0 address
     * @throws SRSInvalidAddress   If there's a problem with an address
     * @throws SRSInvalidHash      When generating/validating the hash
     * @throws SRSInvalidTimestamp Validating timestamp
     */
    public ReversePath forwardShortcut(String address, String forwarder) throws SRSException {
        validateForwarder(forwarder);
        return rewriteForward(new MailAddress(address), forwarder.trim(), SRS0);
    }

    /**
     * Rewrites an address. If it is an email address, a SRS0 is created. If it is an SRS0 address, an SRS1 (Guarded) is
     * created.
     *
     * @param address   Email or srs address
     * @param forwarder The domain of this forwarder
     * @return SRS address
     * @throws SRSInvalidAddress   If there's a problem with an address
     * @throws SRSInvalidHash      When generating/validating the hash
     * @throws SRSInvalidTimestamp Validating timestamp
     */
    public ReversePath forward(String address, String forwarder) throws SRSException {
        validateForwarder(forwarder);
        return rewriteForward(new MailAddress(address), forwarder.trim(), null);
    }

    /**
     * Rewrites an SRS address (SRS0 or SRS1)
     *
     * @param address   The SRS address to reverse
     * @return The rewritten address
     * @throws SRSInvalidAddress   If there's a problem with an address
     * @throws SRSInvalidHash      When generating/validating the hash
     * @throws SRSInvalidTimestamp Validating timestamp
     */
    public ReversePath reverse(String address) throws SRSException {
        return rewriteReverse(new MailAddress(address));
    }

    /**
     * This method parses a valid SRS0 or SRS1 address and returns the source address(the original sender address
     * contained in the srs address). No validation is done, just parse. The address should have a valid srs syntax.
     *
     * @param srsAddress The srs address
     * @return Address of original sender
     * @throws SRSInvalidAddress if parse is not successful
     */
    public static ReversePath asSourceAddress(String srsAddress) throws SRSInvalidAddress {
        SRSAddress srs = SRSAddress.parse(new MailAddress(srsAddress), '=');
        if (srs.isOpaque()) {
            throw new SRSInvalidAddress("Can't parse address " + srsAddress);
        }
        return srs.getSourceAddress();
    }

    private ReversePath rewriteReverse(ReversePath reversePath)
            throws SRSInvalidAddress, SRSInvalidTimestamp, SRSInvalidHash {
        ReversePath result;

        if (PATTERN_SRS0.matcher(reversePath.getLocalPart()).matches()) {
            result = getAddressRewriter(SRS0).reverse(reversePath);
            if (result == null || PATTERN_SRS.matcher(result.getAddress()).matches()) {
                throw new SRSInvalidState("Unable to reverse srs address: " + reversePath);
            }
        } else if (PATTERN_SRS1.matcher(reversePath.getLocalPart()).matches()) {
            result = getAddressRewriter(SRS1).reverse(reversePath);
        } else {
            throw new SRSInvalidAddress("Not an SRS address");
        }

        if (result == null) {
            throw new SRSInvalidState("Unable to reverse srs address: " + reversePath);
        }

        return result;
    }

    private ReversePath rewriteForward(ReversePath reversePath, String forwarder, SRSAddress.Format resultFormat)
            throws SRSInvalidAddress, SRSInvalidHash {

        // Don't rewrite same forwarder addresses if not forced
        if (forwarder.equalsIgnoreCase(reversePath.getDomainPart()) && !options.isAlwaysRewrite()) {
            return reversePath;
        }

        ReversePath result;

        if (PATTERN_SRS.matcher(reversePath.getAddress()).matches()) {
            SRSAddress srsAddress = SRSAddress.parse(reversePath, options.getSeparator());
            if (resultFormat == SRS0) {
                //the result type is forced to SRS0, so we use Shortcut scheme
                if (srsAddress.isSrs0() && srsAddress.isOpaque()) {
                    throw new SRSInvalidAddress("Invalid SRS address for shortcut scheme");
                }
                result = getAddressRewriter(SRS0).forward(new MailAddress(
                        srsAddress.getLocalPart(), srsAddress.getHostname()), forwarder
                );
            } else {
                result = getAddressRewriter(SRS1).forward(reversePath, forwarder);
            }
        } else {
            result = getAddressRewriter(SRS0).forward(
                    new MailAddress(reversePath.getLocalPart(), reversePath.getDomainPart()), forwarder
            );
        }

        if (!PATTERN_SRS.matcher(result.getAddress()).matches()) {
            throw new SRSInvalidState("Unable to rewrite address " + result);
        }
        return result;
    }

    private static void validateForwarder(String forwarder) throws SRSInvalidAddress {
        if (forwarder == null || forwarder.trim().isEmpty() || forwarder.contains("@")) {
            throw new SRSInvalidAddress("Invalid forwarder (alias)");
        }
    }

    SRSOptions getOptions() {
        return options;
    }

    AddressRewriter getAddressRewriter(SRSAddress.Format format) {
        if (format == SRS0) {
            return new Srs0Rewriter(secretKey, getOptions());
        } else if (format == SRS1) {
            return new Srs1Rewriter(secretKey, getOptions());
        }
        throw new SRSInvalidState("Unknown srs version");
    }
}
