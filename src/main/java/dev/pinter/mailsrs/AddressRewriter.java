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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

abstract class AddressRewriter {
    private final SRSOptions options;
    private final String secretKey;

    AddressRewriter(String secretKey, SRSOptions options) {
        this.secretKey = secretKey;
        this.options = options;
    }

    abstract ReversePath forward(ReversePath reversePath, String forwarder) throws SRSInvalidAddress, SRSInvalidHash;

    abstract ReversePath reverse(ReversePath reversePath) throws SRSInvalidHash, SRSInvalidAddress, SRSInvalidTimestamp;

    String calculateHash(int length, String... data) throws SRSInvalidHash {
        if (secretKey == null || secretKey.isEmpty()) {
            throw new SRSInvalidState("SecretKey can't be null");
        }

        String alg = "HmacSHA1";
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), alg);
            Mac mac = Mac.getInstance(alg);
            mac.init(keySpec);

            String hash = Base64.getEncoder().encodeToString(mac.doFinal(
                    Arrays.stream(data).collect(StringBuffer::new, StringBuffer::append, StringBuffer::append)
                            .toString().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
            ));

            if (hash.length() < length) {
                throw new SRSInvalidState(
                        String.format("Configured hashLength is bigger than generated hash (%s characters)",
                                hash.length()));
            }
            return hash.substring(0, length);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new SRSInvalidHash("Error calculating hash", e);
        }
    }

    boolean isValidTimestamp(ZonedDateTime timestamp) {
        if (timestamp == null) {
            return false;
        }

        //subtract 1 from 'now' so isAfter become '>='
        ZonedDateTime startOfLifetime = today().minus(options.getLifetime().plusDays(1));

        //timestamp is valid if >= start-of-lifetime-period and if <= today+1, today is at 00:00:00
        return options.isDisableTimestampValidation() ||
                (timestamp.isAfter(startOfLifetime) && timestamp.isBefore(today().plusDays(2)));
    }

    boolean isInvalidHash(SRSAddress srsAddress) throws SRSInvalidAddress, SRSInvalidHash {
        String hash;
        if (srsAddress.isSrs1()) {
            hash = calculateHash(srsAddress.getHash().length(),
                    srsAddress.getOriginalForwarder(), srsAddress.getOpaquePart());
        } else if (srsAddress.isSrs0() && !srsAddress.isOpaque()) {
            hash = calculateHash(srsAddress.getHash().length(),
                    srsAddress.getTimestamp(), srsAddress.getHostname(), srsAddress.getLocalPart());
        } else {
            throw new SRSInvalidAddress("Not an SRS address");
        }

        return hash.length() < options.getHashMin() || !srsAddress.getHash().equals(hash);
    }

    ZonedDateTime decodeTimestamp(String timestamp) throws SRSInvalidTimestamp {
        if (timestamp.length() != 2) {
            throw new SRSInvalidTimestamp("Timestamp must have 2 characters");
        }

        List<Character> chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());

        //timestamp characters to number of days
        int tsDays = ((chars.indexOf(timestamp.charAt(0)) << 5) | chars.indexOf(timestamp.charAt(1)));
        ZonedDateTime midnight = ZonedDateTime.of(today().toLocalDate(), LocalTime.MIDNIGHT, ZoneOffset.UTC);

        //start of current period
        ZonedDateTime periodStart = midnight
                .minusDays(((today().toEpochSecond() / 86400) % 1024));

        //timestamp in current period
        return periodStart.plusDays(tsDays);
    }

    ZonedDateTime today() {
        return getDateTimeNow().withHour(0).withMinute(0).withSecond(0);
    }

    ZonedDateTime getDateTimeNow() {
        return ZonedDateTime.now();
    }

    String encodeTimestamp(ZonedDateTime time) {
        int ts = (int) (time.toEpochSecond() / (60 * 60 * 24));
        String[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".split("");
        return chars[(ts >> 5) & 0x1F] + chars[ts & 0x1F];
    }

    SRSOptions getOptions() {
        return options;
    }
}
