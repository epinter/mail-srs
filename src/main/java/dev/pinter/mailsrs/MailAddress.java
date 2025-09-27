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

import java.util.Objects;

class MailAddress implements ReversePath {
    private final String localPart;
    private final String domainPart;
    private final String address;

    MailAddress(String localPart, String domainPart) throws SRSInvalidAddress {
        if (isBlankOrSpaces(localPart) || localPart.contains("@")
                || isBlankOrSpaces(domainPart) || domainPart.contains("@")) {
            throw new SRSInvalidAddress("The address cannot be empty and must have one '@'");
        }
        this.localPart = localPart.trim();
        this.domainPart = domainPart.trim();
        this.address = localPart + "@" + domainPart;
    }

    MailAddress(String address) throws SRSInvalidAddress {
        this.address = address == null ? null : address.trim();

        if (isBlankOrSpaces(this.address) || !this.address.contains("@")) {
            throw new SRSInvalidAddress("The address cannot be empty and must have one '@'");
        }


        int iat = this.address.indexOf("@");
        if (iat > 0) {
            this.localPart = this.address.substring(0, iat);
            this.domainPart = this.address.substring(iat + 1);
        } else {
            this.localPart = this.address;
            this.domainPart = null;
        }

        if (this.localPart.contains("@") || (this.domainPart != null && this.domainPart.contains("@"))) {
            throw new SRSInvalidAddress("The address must have only one '@'");
        }
    }

    private static boolean isBlankOrSpaces(String str) {
        return str == null || str.trim().isEmpty() || str.contains(" ");
    }

    @Override
    public String getLocalPart() {
        return localPart;
    }

    @Override
    public String getDomainPart() {
        return domainPart;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MailAddress that = (MailAddress) o;
        return Objects.equals(localPart, that.localPart)
                && Objects.equals(domainPart, that.domainPart)
                && Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localPart, domainPart, address);
    }

    @Override
    public String toString() {
        return address;
    }
}
