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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
class SRSAddress {
    private final Format format;
    private final String opaquePart;
    private final String hash;
    private final String originalForwarder;
    private final String originalHash;
    private final String timestamp;
    private final String hostname;
    private final String localPart;
    private final String forwarder;
    private final String separator;
    private static final Pattern PATTERN_SRS0 = Pattern.compile("(?i)^srs0([=\\-+][^@]+)@(.*)$");
    private static final Pattern PATTERN_SRS1 = Pattern.compile("(?i)^srs1[=\\-+]([^=]+)=([^=]+)=([^@]+)@(.*)");
    private static final Pattern PATTERN_OPAQUEPART = Pattern.compile("(?i)^[=\\-+]([^=]+)=([^=]+)=([^=]+)=([^@]+)$");

    enum Format {
        SRS0,
        SRS1
    }

    private SRSAddress(Builder builder) {
        this.format = builder.format;
        this.opaquePart = builder.opaquePart;
        this.hash = builder.hash;
        this.originalForwarder = builder.originalForwarder;
        this.originalHash = builder.originalHash;
        this.timestamp = builder.timestamp;
        this.hostname = builder.hostname;
        this.localPart = builder.localPart;
        this.forwarder = builder.forwarder;
        this.separator = builder.separator;
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private Format format;
        private String opaquePart;
        private String hash;
        private String originalForwarder;
        private String originalHash;
        private String timestamp;
        private String hostname;
        private String localPart;
        private String forwarder;
        private String separator;

        Builder withFormat(Format format) {
            this.format = format;
            return this;
        }

        Builder withOpaquePart(String opaquePart) {
            this.opaquePart = opaquePart;
            return this;
        }

        Builder withHash(String hash) {
            this.hash = hash;
            return this;
        }

        Builder withOriginalForwarder(String originalForwarder) {
            this.originalForwarder = originalForwarder;
            return this;
        }

        Builder withOriginalHash(String originalHash) {
            this.originalHash = originalHash;
            return this;
        }

        Builder withTimestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        Builder withHostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        Builder withLocalPart(String localPart) {
            this.localPart = localPart;
            return this;
        }

        Builder withForwarder(String forwarder) {
            this.forwarder = forwarder;
            return this;
        }

        Builder withSeparator(char separator) {
            this.separator = String.valueOf(separator);
            return this;
        }

        SRSAddress build() {
            if (this.opaquePart == null) {
                if (format == Format.SRS0) {
                    this.opaquePart = String.format("%s%s=%s=%s=%s", separator, hash, timestamp, hostname, localPart);
                } else if (format == Format.SRS1) {
                    throw new SRSInvalidState("SRS1 needs opaquePart!");
                }
            }
            return new SRSAddress(this);
        }
    }

    String getHash() {
        return hash;
    }

    String getOriginalForwarder() {
        if (format == Format.SRS0) {
            throw new SRSInvalidState("the originalForwarder getter incompatible with SRS0");
        }
        return originalForwarder;
    }

    String getOriginalHash() {
        if (format == Format.SRS0) {
            throw new SRSInvalidState("the originalHash getter incompatible with SRS0");
        }
        return originalHash;
    }

    String getTimestamp() {
        return timestamp;
    }

    String getHostname() {
        if (format == Format.SRS1) {
            throw new SRSInvalidState("the hostname getter incompatible with SRS1");
        }
        return hostname;
    }

    String getLocalPart() {
        if (format == Format.SRS1) {
            throw new SRSInvalidState("the localPart getter incompatible with SRS1");
        }
        return localPart;
    }

    String getOpaquePart() {
        return opaquePart;
    }

    String getForwarder() {
        return forwarder;
    }

    boolean isSrs0() {
        return format == Format.SRS0;
    }

    boolean isSrs1() {
        return format == Format.SRS1;
    }

    boolean isOpaque() {
        if (format == Format.SRS0) {
            return hash == null && timestamp == null && hostname == null && localPart == null;
        } else if (format == Format.SRS1) {
            return originalHash == null && timestamp == null && hostname == null && localPart == null;
        }
        return true;
    }

    ReversePath getSourceAddress() throws SRSInvalidAddress {
        if (isOpaque()) {
            return null;
        }

        return new MailAddress(localPart, hostname);
    }

    static SRSAddress parse(ReversePath reversePath, char separator) throws SRSInvalidAddress {
        String hash;
        String timestamp;
        String hostname;
        String localPart;
        String opaquePart;
        String forwarder;
        String originalForwarder;
        String originalHash;
        Matcher matcherSrs0 = PATTERN_SRS0.matcher(reversePath.getAddress());
        Matcher matcherSrs1 = PATTERN_SRS1.matcher(reversePath.getAddress());
        if (matcherSrs0.find()) {
            if (matcherSrs0.groupCount() == 2) {
                opaquePart = matcherSrs0.group(1);
                forwarder = matcherSrs0.group(2);
            } else {
                throw new SRSInvalidAddress("Invalid srs address");
            }
            if (opaquePart == null || forwarder == null || opaquePart.trim().isEmpty() || forwarder.trim().isEmpty()) {
                throw new SRSInvalidAddress("Invalid srs address");
            }
            Matcher matcher = PATTERN_OPAQUEPART.matcher(opaquePart);
            if (matcher.find()) {
                hash = matcher.group(1);
                timestamp = matcher.group(2);
                hostname = matcher.group(3);
                localPart = matcher.group(4);
                return new SRSAddress.Builder().withFormat(Format.SRS0)
                        .withHash(hash)
                        .withTimestamp(timestamp)
                        .withHostname(hostname)
                        .withLocalPart(localPart)
                        .withForwarder(forwarder)
                        .withSeparator(separator).build();
            } else {
                return new SRSAddress.Builder().withFormat(Format.SRS0)
                        .withOpaquePart(opaquePart)
                        .withForwarder(forwarder)
                        .withSeparator(separator).build();
            }
        } else if (matcherSrs1.find()) {
            if (matcherSrs1.groupCount() == 4) {
                hash = matcherSrs1.group(1);
                originalForwarder = matcherSrs1.group(2);
                opaquePart = matcherSrs1.group(3);
                forwarder = matcherSrs1.group(4);
            } else {
                throw new SRSInvalidAddress("Invalid srs address");
            }
            if (opaquePart == null || forwarder == null || opaquePart.trim().isEmpty() || forwarder.trim().isEmpty()
                    || hash == null || originalForwarder == null || hash.trim().isEmpty()
                    || originalForwarder.trim().isEmpty()) {
                throw new SRSInvalidAddress("Invalid srs address");
            }
            Matcher matcher = PATTERN_OPAQUEPART.matcher(opaquePart);

            if (matcher.find()) {
                originalHash = matcher.group(1);
                timestamp = matcher.group(2);
                hostname = matcher.group(3);
                localPart = matcher.group(4);
            } else {
                //we don't need to parse, so ignore if regex didn't work
                originalHash = null;
                timestamp = null;
                hostname = null;
                localPart = null;
            }

            return new SRSAddress.Builder().withFormat(Format.SRS1)
                    .withHash(hash)
                    .withOriginalForwarder(originalForwarder)
                    .withOpaquePart(opaquePart)
                    .withForwarder(forwarder)
                    .withTimestamp(timestamp)
                    .withOriginalHash(originalHash)
                    .withHostname(hostname)
                    .withLocalPart(localPart)
                    .withSeparator(separator).build();
        }
        throw new SRSInvalidAddress("Invalid srs address");
    }

    ReversePath toReversePath() throws SRSInvalidAddress {
        if (format == Format.SRS0) {
            return new MailAddress(
                    String.format("SRS0%s", opaquePart), forwarder);
        } else if (format == Format.SRS1) {
            return new MailAddress(
                    String.format("SRS1%s%s=%s=%s", separator, hash, originalForwarder, opaquePart), forwarder);
        }

        return null;
    }

    @Override
    public String toString() {
        try {
            ReversePath r = toReversePath();
            return r.getAddress();
        } catch (SRSInvalidAddress ignored) {
        }
        return null;
    }
}
