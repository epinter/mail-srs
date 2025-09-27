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

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static dev.pinter.mailsrs.SRSAddress.Format.SRS0;
import static dev.pinter.mailsrs.SRSAddress.Format.SRS1;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SRSTest {
    private static final String SECRETKEY = "aSecretKey";
    private final Duration MAXAGE = Duration.ofDays(7);
    private final SRSMockTest srs = new SRSMockTest(SECRETKEY, MAXAGE, 4, 4);

    @Test
    public void shouldRewriteForward() throws SRSException {
        String email = "user@example.com";

        SRS srsHashLen = new SRSMockTest(SECRETKEY, MAXAGE, 20, 4);

        assertEquals("SRS0=jA9R=Y6=example.com=user@srs.forward.com",
                srs.forward(email, "srs.forward.com").getAddress());

        assertEquals("SRS0=jA9RX51fAPa9Qe8y6j4F=Y6=example.com=user@srs.forward.com",
                srsHashLen.forward(email, "srs.forward.com").getAddress());
    }

    @Test
    public void shouldReturnSourceAddressIfSrsAddress() throws SRSException {
        assertEquals("user@example.com",
                SRS.asSourceAddress("SRS0=jA9R=Y6=example.com=user@srs.forward.com").getAddress());
        assertEquals("user@example.net",
                SRS.asSourceAddress("SRS1=A1b5=srs.example.org==Pjzr=Y6=example.net=user@srs.forward.com").getAddress());
        assertEquals("user2@example.com",
                SRS.asSourceAddress("SRS0+i+invalid+xj4=Y6=example.com=user2@srs.forward.com").getAddress());
        assertEquals("user@example.com",
                SRS.asSourceAddress("SRS0=jA9R=Y6=example.com=user@srs.forward.com".toLowerCase()).getAddress());
        assertEquals("user@example.net",
                SRS.asSourceAddress("SRS1=A1b5=srs.example.org==Pjzr=Y6=example.net=user@srs.forward.com".toLowerCase()).getAddress());
        assertEquals("user2@example.com",
                SRS.asSourceAddress("SRS0+i+invalid+xj4=Y6=example.com=user2@srs.forward.com".toLowerCase()).getAddress());
        assertThrows(SRSInvalidAddress.class,
                () -> SRS.asSourceAddress("SRS0   =jA9R=Y6=example.com=user@  srs.forward.com").getAddress());
        assertThrows(SRSInvalidAddress.class,
                () -> SRS.asSourceAddress("bounces+SRS=44ldt=IX@example.com").getAddress());
        assertThrows(SRSInvalidAddress.class,
                () -> SRS.asSourceAddress("user@example.com").getAddress());
        assertThrows(SRSInvalidAddress.class,
                () -> SRS.asSourceAddress("SRS0+xx1@srs.example.com").getAddress());
        assertEquals("example.com",
                SRS.asSourceAddress("SRS0=jA9R=Y6=example.com=user@srs.forward.com").getDomainPart());
        assertEquals("user",
                SRS.asSourceAddress("SRS0=jA9R=Y6=example.com=user@srs.forward.com").getLocalPart());
        assertEquals(new MailAddress("user@example.com").getAddress(),
                SRS.asSourceAddress("SRS0=jA9R=Y6=example.com=user@srs.forward.com").getAddress());
        assertEquals(new MailAddress("user@example.com"),
                SRS.asSourceAddress("SRS0=jA9R=Y6=example.com=user@srs.forward.com"));
    }

    @Test
    public void shouldAddressShouldBeEqual() throws SRSInvalidAddress {
        assertEquals(new MailAddress("SRS0=jA9R=Y6=example.com=user@srs.forward.com"),
                new MailAddress("SRS0=jA9R=Y6=example.com=user@srs.forward.com"));
    }

    @Test
    public void shouldRewriteForwardAndReverseNow() throws SRSException {
        String email = "user@example.com";

        SRS srsDefault = new SRS("aKey");
        ReversePath forward = srsDefault.forward(email, "srs.forward.com");

        assertEquals(email, srsDefault.reverse(forward.getAddress()).getAddress());
    }

    @Test
    public void shouldRewriteForwardSRS1AndReverseNow() throws SRSException {
        String email = "u+ser2.2-23ds+1s.@example.com";
        ReversePath forward = srs.forward(email, "srs.forward.org");
        ReversePath forward2 = srs.forward(forward.getAddress(), "srs.example.net");
        ReversePath forward3 = srs.forward(forward2.getAddress(), "srs.example2.org");
        ReversePath forward4 = srs.forward(forward3.getAddress(), "srs.example3.org");
        ReversePath reverse = srs.reverse(forward4.getAddress());

        assertEquals(forward.getAddress(), reverse.getAddress());

        ReversePath origReverse = srs.reverse(reverse.getAddress());
        assertEquals(email, origReverse.getAddress());
    }

    @Test
    public void shouldRewriteForwardSRS1AndReverse() throws SRSException {
        String email = "u+ser2.2-23ds+1s.@example.net";

        SRS srsDefault = new SRS("aKey2");
        ReversePath forward = srsDefault.forward(email, "srs.forward.org");
        ReversePath forward2 = srsDefault.forward(forward.getAddress(), "srs.example.net");
        ReversePath forward3 = srsDefault.forward(forward2.getAddress(), "srs.example2.org");
        ReversePath forward4 = srsDefault.forward(forward3.getAddress(), "srs.example3.org");
        ReversePath reverse = srsDefault.reverse(forward4.getAddress());

        assertEquals(forward.getAddress(), reverse.getAddress());

        ReversePath origReverse = srsDefault.reverse(reverse.getAddress());
        assertEquals(email, origReverse.getAddress());
    }

    @Test
    public void shouldNotRewriteForwardSameForwarderNameSRS0() throws SRSException {
        String address = "SRS0=BInR=Y6=example.net=user@srs.example.org";

        assertEquals(address, srs.forward(address, "srs.example.org").getAddress());

        SRS srsHashLen = new SRSMockTest(SECRETKEY, MAXAGE, 20, 4);
        assertEquals(address, srsHashLen.forward(address, "srs.example.org").getAddress());
    }

    @Test
    public void shouldNotRewriteForwardSameForwarderNameSRS1() throws SRSException {
        String address = "SRS1=A1b5=srs.example.org==Pjzr=Y6=example.net=user@srs.forward.com";

        assertEquals(address, srs.forward(address, "srs.forward.com").getAddress());

        SRS srsHashLen = new SRSMockTest(SECRETKEY, MAXAGE, 20, 4);
        assertEquals(address, srsHashLen.forward(address, "srs.forward.com").getAddress());
    }

    @Test
    public void shouldReverseSRS0() throws SRSException {
        assertEquals("user2@example.com",
                srs.reverse("SRS0=ixj4=Y6=example.com=user2@srs.forward.com").getAddress());

        SRS srsHashLen = new SRSMockTest(SECRETKEY, MAXAGE, 20, 4);

        assertEquals("user2@example.com",
                srsHashLen.reverse("SRS0=ixj4=Y6=example.com=user2@srs.forward.com").getAddress());
    }

    @Test
    public void shouldReverseSRS0SeparatorPlus() throws SRSException {
        assertEquals("user2@example.com",
                srs.reverse("SRS0+ixj4=Y6=example.com=user2@srs.forward.com").getAddress());

        SRS srsHashLen = new SRSMockTest(SECRETKEY, MAXAGE, 20, 4);

        assertEquals("user2@example.com",
                srsHashLen.reverse("SRS0+ixj4=Y6=example.com=user2@srs.forward.com").getAddress());
    }

    @Test
    public void shouldReverseSRS0SeparatorMinus() throws SRSException {
        assertEquals("user2@example.com",
                srs.reverse("SRS0-ixj4=Y6=example.com=user2@srs.forward.com").getAddress());

        SRS srsHashLen = new SRSMockTest(SECRETKEY, MAXAGE, 20, 4);

        assertEquals("user2@example.com",
                srsHashLen.reverse("SRS0-ixj4=Y6=example.com=user2@srs.forward.com").getAddress());
    }

    @Test
    public void shouldReverseSRS1SeparatorPlus() throws SRSException {
        assertEquals("SRS0=Pjzr=Y6=example.net=user@srs.example.org",
                srs.reverse("SRS1+A1b5=srs.example.org==Pjzr=Y6=example.net=user@srs.forward.com").getAddress());

        SRS srsHashLen = new SRSMockTest(SECRETKEY, MAXAGE, 20, 4);

        assertEquals("SRS0=Pjzr=Y6=example.net=user@srs.example.org",
                srsHashLen.reverse("SRS1+A1b5=srs.example.org==Pjzr=Y6=example.net=user@srs.forward.com").getAddress());
    }

    @Test
    public void shouldReverseSRS1SeparatorMinus() throws SRSException {
        assertEquals("SRS0=Pjzr=Y6=example.net=user@srs.example.org",
                srs.reverse("SRS1-A1b5=srs.example.org==Pjzr=Y6=example.net=user@srs.forward.com").getAddress());

        SRS srsHashLen = new SRSMockTest(SECRETKEY, MAXAGE, 20, 4);

        assertEquals("SRS0=Pjzr=Y6=example.net=user@srs.example.org",
                srsHashLen.reverse("SRS1-A1b5=srs.example.org==Pjzr=Y6=example.net=user@srs.forward.com").getAddress());
    }

    @Test
    public void shouldRewriteForwardShortcut() throws SRSException {
        String email = "SRS0=BInR=Y6=example.net=user@srs.example.org";

        ReversePath shortcut = srs.forwardShortcut(email, "srs.example.net");
        assertEquals("SRS0=Pjzr=Y6=example.net=user@srs.example.net", shortcut.getAddress());
        assertEquals("user@example.net", srs.reverse(shortcut.getAddress()).getAddress());

        SRS srsHashLen = new SRSMockTest(SECRETKEY, MAXAGE, 20, 4);
        String shortcut2 = srsHashLen.forwardShortcut(email, "srs.example.net").getAddress();
        assertEquals("SRS0=PjzrbDR9TWSwCIFb6YvQ=Y6=example.net=user@srs.example.net", shortcut2);
        assertEquals("user@example.net", srs.reverse(shortcut2).getAddress());
    }

    @Test
    public void shouldThrowIfNotSRSAddress() {
        String email = "user@example.com";

        assertThrows(SRSInvalidAddress.class, () -> srs.reverse(email));
    }

    @Test
    public void shouldRewriteForwardGuarded() throws SRSException {
        String email = "SRS0=BInR=Y6=example.net=user@srs.example.org";

        assertEquals("SRS1=D1w/=srs.example.org==BInR=Y6=example.net=user@srs.example.net",
                srs.forward(email, "srs.example.net").getAddress());

        SRS srsHashLen = new SRSMockTest(SECRETKEY, MAXAGE, 20, 4);
        assertEquals("SRS1=D1w/R4+He4nt5hgt0mAc=srs.example.org==BInR=Y6=example.net=user@srs.example.net",
                srsHashLen.forward(email, "srs.example.net").getAddress());
    }

    @Test
    public void shouldRewriteForwardSRS1() throws SRSException {
        String address = "SRS1=A1b5=srs.example.org==Pjzr=Y6=example.net=user@srs.forward.com";

        assertEquals("SRS1=A1b5=srs.example.org==Pjzr=Y6=example.net=user@srs.forward.org",
                srs.forward(address, "srs.forward.org").getAddress());

        SRS srsHashLen = new SRSMockTest(SECRETKEY, MAXAGE, 20, 4);
        assertEquals("SRS1=A1b5ybwUfPRo0LXSoWX9=srs.example.org==Pjzr=Y6=example.net=user@srs.forward.net",
                srsHashLen.forward(address, "srs.forward.net").getAddress());
    }

    @Test
    public void shouldPassIfSpaceAtStartOrEnd() throws SRSException {
        String address = "    SRS1=A1b5=srs.example.org==Pjzr=Y6=example.net=user@srs.forward.com   ";

        assertEquals("SRS1=A1b5=srs.example.org==Pjzr=Y6=example.net=user@srs.forward.org",
                srs.forward(address, "srs.forward.org").getAddress());
    }

    @Test
    public void shouldFailIfTwoAt() {
        String address = "    SRS1=A1b5=srs.example.org==Pjzr=Y6=exa@mple.net=user@srs.forward.com   ";
        String address2 = "    SRS1=A1b5=srs.example.org==Pjzr=Y6=example.net=user@srs.forw@ard.com   ";

        assertThrows(SRSInvalidAddress.class, () -> srs.forward(address, "srs.forward.org"));
        assertThrows(SRSInvalidAddress.class, () -> srs.forward(address2, "srs.forward.org"));
        assertThrows(SRSInvalidAddress.class, () -> srs.reverse(address));
        assertThrows(SRSInvalidAddress.class, () -> srs.reverse(address2));
    }

    @Test
    public void shouldFailIfEmptyForwarder() {
        String address = "SRS1=A1b5=srs.example.org==Pjzr=Y6=example.net=user@srs.forward.com";
        String address2 = "user@example.com";

        assertThrows(SRSInvalidAddress.class, () -> srs.forward(address, ""));
        assertThrows(SRSInvalidAddress.class, () -> srs.forward(address, "   "));
        assertThrows(SRSInvalidAddress.class, () -> srs.forward(address, "  user@example.com  "));
        assertThrows(SRSInvalidAddress.class, () -> srs.forward(address, null));

        assertThrows(SRSInvalidAddress.class, () -> srs.forward(address2, ""));
        assertThrows(SRSInvalidAddress.class, () -> srs.forward(address2, "   "));
        assertThrows(SRSInvalidAddress.class, () -> srs.forward(address2, "  user@example.com  "));
        assertThrows(SRSInvalidAddress.class, () -> srs.forward(address2, null));
    }

    @Test
    public void shouldFailIfEmptyAddress() {
        assertThrows(SRSInvalidAddress.class, () -> srs.forward(null, "srs.forward.org"));
        assertThrows(SRSInvalidAddress.class, () -> srs.reverse(null));
        assertThrows(SRSInvalidAddress.class, () -> srs.forward("  ", "srs.forward.org"));
        assertThrows(SRSInvalidAddress.class, () -> srs.reverse("  "));
        assertThrows(SRSInvalidAddress.class, () -> srs.forward("", "srs.forward.org"));
        assertThrows(SRSInvalidAddress.class, () -> srs.reverse(""));
        assertThrows(SRSInvalidAddress.class, () -> srs.forward("@", "srs.forward.org"));
        assertThrows(SRSInvalidAddress.class, () -> srs.reverse("@"));
        assertThrows(SRSInvalidAddress.class, () -> srs.forward("@example.com", "srs.forward.org"));
        assertThrows(SRSInvalidAddress.class, () -> srs.reverse("@example.com"));
        assertThrows(SRSInvalidAddress.class, () -> srs.forward("user@", "srs.forward.org"));
        assertThrows(SRSInvalidAddress.class, () -> srs.reverse("user@"));
        assertThrows(SRSInvalidAddress.class, () -> srs.forward("SRS1=A1b5=srs.example.org==Pjzr=Y6=example.net=user@", "srs.forward.org"));
        assertThrows(SRSInvalidAddress.class, () -> srs.reverse("SRS1=A1b5=srs.example.org==Pjzr=Y6=example.net=user@"));
        assertThrows(SRSInvalidAddress.class, () -> srs.forward("SRS0=BInR=Y6=example.net=user@srs.example.org@", "srs.forward.org"));
        assertThrows(SRSInvalidAddress.class, () -> srs.reverse("SRS0=BInR=Y6=example.net=user@srs.example.org@"));
        assertThrows(SRSInvalidAddress.class, () -> srs.forward("@srs.forward.com", "srs.forward.org"));
        assertThrows(SRSInvalidAddress.class, () -> srs.reverse("@srs.forward.com"));
    }

    @Test
    public void shouldNotFailFutureReverseSRS1() throws SRSException {
        String address = "SRS1=wtPD=srs.forward.org==/pRY=5E=example.com=u+ser2.2-23ds+1s.@srs.example.net";

        assertEquals("SRS0=/pRY=5E=example.com=u+ser2.2-23ds+1s.@srs.forward.org", srs.reverse(address).getAddress());

        SRS srsHashLen = new SRSMockTest(SECRETKEY, MAXAGE, 20, 4);
        assertEquals("SRS0=/pRY=5E=example.com=u+ser2.2-23ds+1s.@srs.forward.org", srsHashLen.reverse(address).getAddress());
    }

    @Test
    public void shouldFailIfAddressInvalid() {
        assertThrows(SRSInvalidAddress.class, () -> srs.reverse("user"));
        assertThrows(SRSInvalidAddress.class, () -> srs.forward("user", "forward.com"));
    }

    @Test
    public void shouldFailPast() {
        String address = "SRS0=R7m8=G3=example.net=user@srs.example.org";
        assertThrows(SRSInvalidTimestamp.class, () -> srs.reverse(address));
    }

    @Test
    public void shouldNotFailPastWithValidationDisabledSRS0() {
        String address = "SRS0=R7m8=G3=example.net=user@srs.example.org";
        SRS srs2 = new SRSMockTest(SECRETKEY, SRSOptions.builder().withLifetime(MAXAGE)
                .withDisableTimestampValidation(true).build());
        assertDoesNotThrow(() -> srs2.reverse(address));
    }

    @Test
    public void shouldForwardNotFailPastWithValidationDisabledSRS1()  {
        String address = "SRS1=wtPD=srs.forward.org==/pRY=5E=example.com=u+ser2.2-23ds+1s.@srs.example.net";
        SRS srs2 = new SRSMockTest(SECRETKEY, SRSOptions.builder().withLifetime(MAXAGE)
                .withDisableTimestampValidation(true)
                .withTryVerifySrs1Time(true).build());
        assertDoesNotThrow(() -> srs2.reverse(address));
    }

    @Test
    public void shouldForwardUnknownSRS() throws SRSException {
        String address1 = "SRS0+xx1@srs.example.com";
        SRS srs2 = new SRSMockTest(SECRETKEY, SRSOptions.builder().withLifetime(MAXAGE).build());
        ReversePath fwd = srs2.forward(address1, "srs.example.org");
        assertEquals("SRS1=A1No=srs.example.com=+xx1@srs.example.org", srs2.forward(address1,
                "srs.example.org").getAddress());
        assertDoesNotThrow(() -> srs2.reverse(fwd.getAddress()));
    }

    @Test
    public void shouldFailOnShortcutUnknownSRS0() {
        String address1 = "SRS0+xx1@srs.example.com";
        SRS srs2 = new SRSMockTest(SECRETKEY, SRSOptions.builder().withLifetime(MAXAGE).build());
        assertThrows(SRSInvalidAddress.class, () -> srs2.forwardShortcut(address1, "srs.example.org"));
    }

    @Test
    public void shouldPassWeirdAddress() throws SRSException {
        String email = "AasSRS0=as=SRS1=z==a=s+a+1++1z=a@example.com";
        ReversePath fwd = srs.forward(email, "srs.example.org");
        assertEquals("SRS0=3tPG=Y6=example.com=AasSRS0=as=SRS1=z==a=s+a+1++1z=a@srs.example.org", fwd.getAddress());
        assertEquals(email, srs.reverse(fwd.getAddress()).getAddress());
    }

    @Test
    public void shouldForwardWithAddressWithPlus() throws SRSException {
        String email = "bounces+SRS=44ldt=IX@example.com";
        ReversePath fwd = srs.forward(email, "srs.forward.org");
        assertEquals("SRS0=dU01=Y6=example.com=bounces+SRS=44ldt=IX@srs.forward.org", fwd.getAddress());
        assertEquals(email, srs.reverse(fwd.getAddress()).getAddress());
    }

    @Test
    public void shouldFailWithIncompatibleSRS() {
        String email = "bounces+SRS=44ldt=IX@contoso.com";
        assertThrows(SRSInvalidAddress.class, () -> srs.reverse(email));
    }

    @Test
    public void shouldPassNextDay() throws SRSException {
        String address = "SRS0=jKZM=Y7=example.com=user@srs.forward.com";
        assertEquals("user@example.com", srs.reverse(address).getAddress());
    }

    @Test
    public void shouldFailTwoDaysAfter() {
        String address = "SRS0=P/DW=ZA=example.com=user@srs.forward.com";
        assertThrows(SRSInvalidTimestamp.class, () -> srs.reverse(address));
    }

    @Test
    public void shouldForwardFailPastSRS1() {
        String address = "SRS1=wtPD=srs.forward.org==/pRY=5E=example.com=u+ser2.2-23ds+1s.@srs.example.net";
        SRS srs2 = new SRSMockTest(SECRETKEY, SRSOptions
                .builder().withTryVerifySrs1Time(true).withLifetime(MAXAGE).build());
        assertThrows(SRSInvalidTimestamp.class, () -> srs2.reverse(address));
    }

    @Test
    public void shouldFailBeforeMaxAgeDay() {
        //now=2025-06-15T15:06:40Z(unixtime 1750000000) timestamp=2025-05-15T00:00Z
        String address = "SRS0=Wj4r=X7=example.net=user@srs.example.org";
        SRSMockTest srs2 = new SRSMockTest(SECRETKEY, SRSOptions
                .builder().withTryVerifySrs1Time(true).withLifetime(Duration.ofDays(30)).build());
        assertThrows(SRSInvalidTimestamp.class, () -> srs2.reverse(address));
    }

    @Test
    public void shouldReverseGuarded() throws SRSException {
        String address = "SRS1=D1w/=srs.example.org==BInR=Y6=example.net=user@srs.example.net";

        assertEquals("SRS0=BInR=Y6=example.net=user@srs.example.org", srs.reverse(address).getAddress());

        SRS srsHashLen = new SRSMockTest(SECRETKEY, MAXAGE, 20, 4);
        assertEquals("SRS0=BInR=Y6=example.net=user@srs.example.org", srsHashLen.reverse(address).getAddress());
    }

    @Test
    public void shouldReverseGuardedChained() throws SRSException {
        String address = "SRS1=A1b5=srs.example.org==Pjzr=Y6=example.net=user@srs.forward.com";

        ReversePath rev = srs.reverse(address);
        assertEquals("SRS0=Pjzr=Y6=example.net=user@srs.example.org", rev.getAddress());
        assertEquals("user@example.net", srs.reverse(rev.getAddress()).getAddress());

        SRS srsHashLen = new SRSMockTest(SECRETKEY, MAXAGE, 20, 4);
        ReversePath rev2 = srsHashLen.reverse(address);
        assertEquals("SRS0=Pjzr=Y6=example.net=user@srs.example.org", rev2.getAddress());
        assertEquals("user@example.net", srsHashLen.reverse(rev2.getAddress()).getAddress());
    }

    @Test
    public void shouldFailInvalidHash() {
        SRS srsHash = new SRSMockTest("anotherKey", MAXAGE, 20, 4);

        assertThrows(SRSInvalidHash.class,
                () -> srsHash.reverse("SRS0=ixj4=Y6=example.com=user2@srs.forward.com"));

        assertThrows(SRSInvalidHash.class,
                () -> srsHash.reverse("SRS1=D1w/=srs.example.org==BInR=Y6=example.net=user@srs.example.net"));
    }

    @Test
    public void shouldNotMatchHashAndTimestamp() throws SRSException {
        srs.setTestInstant(ZonedDateTime.ofInstant(Instant.ofEpochSecond(1700000000), ZoneOffset.UTC));
        String email = "user@example.com";

        assertNotEquals("SRS0=jA9R=Y6=example.com=user@srs.forward.com",
                srs.forward(email, "srs.forward.com").getAddress());

        SRSMockTest srsHashLen = new SRSMockTest(SECRETKEY, MAXAGE, 20, 4);
        srsHashLen.setTestInstant(ZonedDateTime.ofInstant(Instant.ofEpochSecond(1700000000), ZoneOffset.UTC));

        assertNotEquals("SRS0=jA9RX51fAPa9Qe8y6j4F=Y6=example.com=user@srs.forward.com",
                srsHashLen.forward(email, "srs.forward.com").getAddress());
    }

    private static class SRSMockTest extends SRS {
        private final MockSrs0Rewriter srs0Rewriter;
        private final MockSrs1Rewriter srs1Rewriter;

        public SRSMockTest(String secretKey, Duration lifetime, int hashLength, int hashMin) {
            this(secretKey,
                    SRSOptions.builder().withLifetime(lifetime).withHashLength(hashLength).withHashMin(hashMin).build()
            );
        }

        public SRSMockTest(String secretKey, SRSOptions options) {
            super(secretKey, options);
            srs0Rewriter = new MockSrs0Rewriter(secretKey, getOptions());
            srs1Rewriter = new MockSrs1Rewriter(secretKey, getOptions());
        }


        public void setTestInstant(ZonedDateTime testInstant) {
            if (srs0Rewriter != null) {
                srs0Rewriter.setTestInstant(testInstant);
            }
            if (srs1Rewriter != null) {
                srs1Rewriter.setTestInstant(testInstant);
            }
        }

        @Override
        protected AddressRewriter getAddressRewriter(SRSAddress.Format format) {
            if (format == SRS0) {
                return srs0Rewriter;
            } else if (format == SRS1) {
                return srs1Rewriter;
            }
            throw new SRSInvalidState("Unknown srs version");
        }
    }

    private static class MockSrs0Rewriter extends Srs0Rewriter {
        private ZonedDateTime testInstant = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1750000000), ZoneOffset.UTC);

        public MockSrs0Rewriter(String secretKey, SRSOptions options) {
            super(secretKey, options);
        }

        public void setTestInstant(ZonedDateTime testInstant) {
            this.testInstant = testInstant;
        }

        @Override
        protected ZonedDateTime getDateTimeNow() {
            return testInstant;
        }
    }

    private static class MockSrs1Rewriter extends Srs1Rewriter {
        private ZonedDateTime testInstant = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1750000000), ZoneOffset.UTC);

        public MockSrs1Rewriter(String secretKey, SRSOptions options) {
            super(secretKey, options);
        }

        public void setTestInstant(ZonedDateTime testInstant) {
            this.testInstant = testInstant;
        }

        @Override
        protected ZonedDateTime getDateTimeNow() {
            return testInstant;
        }
    }
}
