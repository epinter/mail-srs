package dev.pinter.mailsrs;

/**
 * A representation of an email or an SRS address
 */
public interface ReversePath {
    /**
     * @return Local part of an address
     */
    String getLocalPart();

    /**
     * @return Domain part of an address
     */
    String getDomainPart();

    /**
     * @return Full address
     */
    String getAddress();
}
