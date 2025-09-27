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

import static dev.pinter.mailsrs.SRSAddress.Format.SRS0;

class Srs0Rewriter extends AddressRewriter {
    Srs0Rewriter(String secretKey, SRSOptions options) {
        super(secretKey, options);
    }

    @Override
    ReversePath forward(ReversePath reversePath, String forwarder) throws SRSInvalidHash, SRSInvalidAddress {
        String ts = encodeTimestamp(today());
        return SRSAddress.builder()
                .withFormat(SRS0)
                .withHash(calculateHash(getOptions().getHashLength(),
                        ts, reversePath.getDomainPart(), reversePath.getLocalPart()))
                .withTimestamp(ts)
                .withHostname(reversePath.getDomainPart())
                .withLocalPart(reversePath.getLocalPart())
                .withForwarder(forwarder)
                .withSeparator(getOptions().getSeparator()).build().toReversePath();
    }

    @Override
    ReversePath reverse(ReversePath reversePath) throws SRSInvalidAddress, SRSInvalidHash, SRSInvalidTimestamp {
        SRSAddress srsAddress = SRSAddress.parse(reversePath, getOptions().getSeparator());
        if (isInvalidHash(srsAddress)) {
            throw new SRSInvalidHash("Invalid hash for address "
                    + srsAddress.getOpaquePart() + " " + srsAddress.getForwarder());
        }
        if (isValidTimestamp(decodeTimestamp(srsAddress.getTimestamp()))) {
            return new MailAddress(srsAddress.getLocalPart(), srsAddress.getHostname());
        } else {
            throw new SRSInvalidTimestamp("Invalid timestamp "
                    + srsAddress.getTimestamp() + " for address " + srsAddress);
        }
    }
}
