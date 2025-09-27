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
import static dev.pinter.mailsrs.SRSAddress.Format.SRS1;

class Srs1Rewriter extends AddressRewriter {
    Srs1Rewriter(String secretKey, SRSOptions options) {
        super(secretKey, options);
    }

    @Override
    ReversePath forward(ReversePath reversePath, String forwarder) throws SRSInvalidAddress, SRSInvalidHash {
        SRSAddress origSrs = SRSAddress.parse(reversePath, getOptions().getSeparator());
        String origForwarder;
        if (origSrs.isSrs0()) {
            origForwarder = origSrs.getForwarder();
        } else if (origSrs.isSrs1()) {
            origForwarder = origSrs.getOriginalForwarder();
        } else {
            throw new SRSInvalidAddress("The source address must be a SRS0 or SRS1");
        }

        return SRSAddress.builder()
                .withFormat(SRS1)
                .withHash(calculateHash(getOptions().getHashLength(), origForwarder, origSrs.getOpaquePart()))
                .withOriginalForwarder(origForwarder)
                .withOpaquePart(origSrs.getOpaquePart())
                .withForwarder(forwarder)
                .withSeparator(getOptions().getSeparator()).build().toReversePath();
    }

    @Override
    ReversePath reverse(ReversePath reversePath) throws SRSInvalidHash, SRSInvalidAddress, SRSInvalidTimestamp {
        SRSAddress srsAddress = SRSAddress.parse(reversePath, getOptions().getSeparator());
        if (isInvalidHash(srsAddress)) {
            throw new SRSInvalidHash("Invalid hash " + srsAddress.getHash() + " for address " + srsAddress);
        }

        boolean validateTimestamp = false;
        if (srsAddress.isSrs0()) {
            validateTimestamp = true;
        } else if (srsAddress.isSrs1() && getOptions().isTryVerifySrs1Time() && srsAddress.getTimestamp() != null) {
            validateTimestamp = true;
        }

        if (!validateTimestamp || isValidTimestamp(decodeTimestamp(srsAddress.getTimestamp()))) {
            return SRSAddress.builder()
                    .withFormat(SRS0)
                    .withOpaquePart(srsAddress.getOpaquePart())
                    .withForwarder(srsAddress.getOriginalForwarder())
                    .withSeparator(getOptions().getSeparator()).build().toReversePath();
        } else {
            throw new SRSInvalidTimestamp("Invalid timestamp "
                    + srsAddress.getTimestamp() + " for address " + srsAddress);
        }
    }
}
