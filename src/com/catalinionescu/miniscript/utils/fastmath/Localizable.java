package com.catalinionescu.miniscript.utils.fastmath;

import java.io.Serializable;
import java.util.Locale;

/**
 * Interface for localizable strings.
 *
 * @since 2.2
 */
public interface Localizable extends Serializable {
    /**
     * Gets the source (non-localized) string.
     *
     * @return the source string.
     */
    String getSourceString();

    /**
     * Gets the localized string.
     *
     * @param locale locale into which to get the string.
     * @return the localized string or the source string if no
     * localized version is available.
     */
    String getLocalizedString(Locale locale);
}
