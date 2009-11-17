/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.pim.vcard;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for VCard handling codes.
 */
public class VCardUtils {
    // Note that not all types are included in this map/set, since, for example, TYPE_HOME_FAX is
    // converted to two parameter Strings. These only contain some minor fields valid in both
    // vCard and current (as of 2009-08-07) Contacts structure. 
    private static final Map<Integer, String> sKnownPhoneTypesMap_ItoS;
    private static final Set<String> sPhoneTypesUnknownToContactsSet;
    private static final Map<String, Integer> sKnownPhoneTypeMap_StoI;
    private static final Map<Integer, String> sKnownImPropNameMap_ItoS;
    private static final Set<String> sMobilePhoneLabelSet;

    static {
        sKnownPhoneTypesMap_ItoS = new HashMap<Integer, String>();
        sKnownPhoneTypeMap_StoI = new HashMap<String, Integer>();

        sKnownPhoneTypesMap_ItoS.put(Phone.TYPE_CAR, VCardConstants.PARAM_TYPE_CAR);
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_TYPE_CAR, Phone.TYPE_CAR);
        sKnownPhoneTypesMap_ItoS.put(Phone.TYPE_PAGER, VCardConstants.PARAM_TYPE_PAGER);
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_TYPE_PAGER, Phone.TYPE_PAGER);
        sKnownPhoneTypesMap_ItoS.put(Phone.TYPE_ISDN, VCardConstants.PARAM_TYPE_ISDN);
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_TYPE_ISDN, Phone.TYPE_ISDN);
        
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_TYPE_HOME, Phone.TYPE_HOME);
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_TYPE_WORK, Phone.TYPE_WORK);
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_TYPE_CELL, Phone.TYPE_MOBILE);
                
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_PHONE_EXTRA_TYPE_OTHER, Phone.TYPE_OTHER);
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_PHONE_EXTRA_TYPE_CALLBACK,
                Phone.TYPE_CALLBACK);
        sKnownPhoneTypeMap_StoI.put(
                VCardConstants.PARAM_PHONE_EXTRA_TYPE_COMPANY_MAIN, Phone.TYPE_COMPANY_MAIN);
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_PHONE_EXTRA_TYPE_RADIO, Phone.TYPE_RADIO);
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_PHONE_EXTRA_TYPE_TTY_TDD,
                Phone.TYPE_TTY_TDD);
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_PHONE_EXTRA_TYPE_ASSISTANT,
                Phone.TYPE_ASSISTANT);

        sPhoneTypesUnknownToContactsSet = new HashSet<String>();
        sPhoneTypesUnknownToContactsSet.add(VCardConstants.PARAM_TYPE_MODEM);
        sPhoneTypesUnknownToContactsSet.add(VCardConstants.PARAM_TYPE_BBS);
        sPhoneTypesUnknownToContactsSet.add(VCardConstants.PARAM_TYPE_VIDEO);

        sKnownImPropNameMap_ItoS = new HashMap<Integer, String>();
        sKnownImPropNameMap_ItoS.put(Im.PROTOCOL_AIM, VCardConstants.PROPERTY_X_AIM);
        sKnownImPropNameMap_ItoS.put(Im.PROTOCOL_MSN, VCardConstants.PROPERTY_X_MSN);
        sKnownImPropNameMap_ItoS.put(Im.PROTOCOL_YAHOO, VCardConstants.PROPERTY_X_YAHOO);
        sKnownImPropNameMap_ItoS.put(Im.PROTOCOL_SKYPE, VCardConstants.PROPERTY_X_SKYPE_USERNAME);
        sKnownImPropNameMap_ItoS.put(Im.PROTOCOL_GOOGLE_TALK,
                VCardConstants.PROPERTY_X_GOOGLE_TALK);
        sKnownImPropNameMap_ItoS.put(Im.PROTOCOL_ICQ, VCardConstants.PROPERTY_X_ICQ);
        sKnownImPropNameMap_ItoS.put(Im.PROTOCOL_JABBER, VCardConstants.PROPERTY_X_JABBER);
        sKnownImPropNameMap_ItoS.put(Im.PROTOCOL_QQ, VCardConstants.PROPERTY_X_QQ);
        sKnownImPropNameMap_ItoS.put(Im.PROTOCOL_NETMEETING, VCardConstants.PROPERTY_X_NETMEETING);

        // \u643A\u5E2F\u96FB\u8A71 = Full-width Hiragana "Keitai-Denwa" (mobile phone)
        // \u643A\u5E2F = Full-width Hiragana "Keitai" (mobile phone)
        // \u30B1\u30A4\u30BF\u30A4 = Full-width Katakana "Keitai" (mobile phone)
        // \uFF79\uFF72\uFF80\uFF72 = Half-width Katakana "Keitai" (mobile phone)
        sMobilePhoneLabelSet = new HashSet<String>(Arrays.asList(
                "MOBILE", "\u643A\u5E2F\u96FB\u8A71", "\u643A\u5E2F", "\u30B1\u30A4\u30BF\u30A4",
                "\uFF79\uFF72\uFF80\uFF72"));
    }

    public static String getPhoneTypeString(Integer type) {
        return sKnownPhoneTypesMap_ItoS.get(type);
    }

    /**
     * Returns Interger when the given types can be parsed as known type. Returns String object
     * when not, which should be set to label. 
     */
    public static Object getPhoneTypeFromStrings(Collection<String> types,
            String number) {
        if (number == null) {
            number = "";
        }
        int type = -1;
        String label = null;
        boolean isFax = false;
        boolean hasPref = false;
        
        if (types != null) {
            for (String typeString : types) {
                if (typeString == null) {
                    continue;
                }
                typeString = typeString.toUpperCase();
                if (typeString.equals(VCardConstants.PARAM_TYPE_PREF)) {
                    hasPref = true;
                } else if (typeString.equals(VCardConstants.PARAM_TYPE_FAX)) {
                    isFax = true;
                } else {
                    if (typeString.startsWith("X-") && type < 0) {
                        typeString = typeString.substring(2);
                    }
                    if (typeString.length() == 0) {
                        continue;
                    }
                    final Integer tmp = sKnownPhoneTypeMap_StoI.get(typeString);
                    if (tmp != null) {
                        final int typeCandidate = tmp;
                        // TYPE_PAGER is prefered when the number contains @ surronded by
                        // a pager number and a domain name.
                        // e.g.
                        // o 1111@domain.com
                        // x @domain.com
                        // x 1111@
                        final int indexOfAt = number.indexOf("@");
                        if ((typeCandidate == Phone.TYPE_PAGER
                                && 0 < indexOfAt && indexOfAt < number.length() - 1)
                                || type < 0
                                || type == Phone.TYPE_CUSTOM) {
                            type = tmp;
                        }
                    } else if (type < 0) {
                        type = Phone.TYPE_CUSTOM;
                        label = typeString;
                    }
                }
            }
        }
        if (type < 0) {
            if (hasPref) {
                type = Phone.TYPE_MAIN;
            } else {
                // default to TYPE_HOME
                type = Phone.TYPE_HOME;
            }
        }
        if (isFax) {
            if (type == Phone.TYPE_HOME) {
                type = Phone.TYPE_FAX_HOME;
            } else if (type == Phone.TYPE_WORK) {
                type = Phone.TYPE_FAX_WORK;
            } else if (type == Phone.TYPE_OTHER) {
                type = Phone.TYPE_OTHER_FAX;
            }
        }
        if (type == Phone.TYPE_CUSTOM) {
            return label;
        } else {
            return type;
        }
    }

    @SuppressWarnings("deprecation")
    public static boolean isMobilePhoneLabel(final String label) {
        // For backward compatibility.
        // Detail: Until Donut, there isn't TYPE_MOBILE for email while there is now.
        //         To support mobile type at that time, this custom label had been used.
        return (android.provider.Contacts.ContactMethodsColumns.MOBILE_EMAIL_TYPE_NAME.equals(label)
                || sMobilePhoneLabelSet.contains(label));
    }

    public static boolean isValidInV21ButUnknownToContactsPhoteType(final String label) {
        return sPhoneTypesUnknownToContactsSet.contains(label);
    }

    public static String getPropertyNameForIm(final int protocol) {
        return sKnownImPropNameMap_ItoS.get(protocol);
    }

    public static String[] sortNameElements(final int vcardType,
            final String familyName, final String middleName, final String givenName) {
        final String[] list = new String[3];
        final int nameOrderType = VCardConfig.getNameOrderType(vcardType);
        switch (nameOrderType) {
            case VCardConfig.NAME_ORDER_JAPANESE: {
                if (containsOnlyPrintableAscii(familyName) &&
                        containsOnlyPrintableAscii(givenName)) {
                    list[0] = givenName;
                    list[1] = middleName;
                    list[2] = familyName;
                } else {
                    list[0] = familyName;
                    list[1] = middleName;
                    list[2] = givenName;
                }
                break;
            }
            case VCardConfig.NAME_ORDER_EUROPE: {
                list[0] = middleName;
                list[1] = givenName;
                list[2] = familyName;
                break;
            }
            default: {
                list[0] = givenName;
                list[1] = middleName;
                list[2] = familyName;
                break;
            }
        }
        return list;
    }

    public static int getPhoneNumberFormat(final int vcardType) {
        if (VCardConfig.isJapaneseDevice(vcardType)) {
            return PhoneNumberUtils.FORMAT_JAPAN;
        } else {
            return PhoneNumberUtils.FORMAT_NANP;
        }
    }

    /**
     * Inserts postal data into the builder object.
     * 
     * Note that the data structure of ContactsContract is different from that defined in vCard.
     * So some conversion may be performed in this method. See also
     * {{@link #getVCardPostalElements(ContentValues)}
     */
    public static void insertStructuredPostalDataUsingContactsStruct(int vcardType,
            final ContentProviderOperation.Builder builder,
            final VCardEntry.PostalData postalData) {
        builder.withValueBackReference(StructuredPostal.RAW_CONTACT_ID, 0);
        builder.withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);

        builder.withValue(StructuredPostal.TYPE, postalData.type);
        if (postalData.type == StructuredPostal.TYPE_CUSTOM) {
            builder.withValue(StructuredPostal.LABEL, postalData.label);
        }

        builder.withValue(StructuredPostal.POBOX, postalData.pobox);
        // TODO: Japanese phone seems to use this field for expressing all the address including
        // region, city, etc. Not sure we're ok to store them into NEIGHBORHOOD, while it would be
        // better than dropping them all.
        builder.withValue(StructuredPostal.NEIGHBORHOOD, postalData.extendedAddress);
        builder.withValue(StructuredPostal.STREET, postalData.street);
        builder.withValue(StructuredPostal.CITY, postalData.localty);
        builder.withValue(StructuredPostal.REGION, postalData.region);
        builder.withValue(StructuredPostal.POSTCODE, postalData.postalCode);
        builder.withValue(StructuredPostal.COUNTRY, postalData.country);

        builder.withValue(StructuredPostal.FORMATTED_ADDRESS,
                postalData.getFormattedAddress(vcardType));
        if (postalData.isPrimary) {
            builder.withValue(Data.IS_PRIMARY, 1);
        }
    }

    /**
     * Returns String[] containing address information based on vCard spec
     * (PO Box, Extended Address, Street, Locality, Region, Postal Code, Country Name).
     * All String objects are non-null ("" is used when the relevant data is empty).
     *
     * Note that the data structure of ContactsContract is different from that defined in vCard.
     * So some conversion may be performed in this method. See also
     * {{@link #insertStructuredPostalDataUsingContactsStruct(int,
     * android.content.ContentProviderOperation.Builder,
     * android.pim.vcard.VCardEntry.PostalData)}
     */
    public static String[] getVCardPostalElements(ContentValues contentValues) {
        // adr-value    = 0*6(text-value ";") text-value
        //              ; PO Box, Extended Address, Street, Locality, Region, Postal
        //              ; Code, Country Name
        String[] dataArray = new String[7];
        dataArray[0] = contentValues.getAsString(StructuredPostal.POBOX);
        if (dataArray[0] == null) {
            dataArray[0] = "";
        }
        // We keep all the data in StructuredPostal, presuming NEIGHBORHOOD is
        // similar to "Extended Address".
        dataArray[1] = contentValues.getAsString(StructuredPostal.NEIGHBORHOOD);
        if (dataArray[1] == null) {
            dataArray[1] = "";
        }
        dataArray[2] = contentValues.getAsString(StructuredPostal.STREET);
        if (dataArray[2] == null) {
            dataArray[2] = "";
        }
        // Assume that localty == city
        dataArray[3] = contentValues.getAsString(StructuredPostal.CITY);
        if (dataArray[3] == null) {
            dataArray[3] = "";
        }
        String region = contentValues.getAsString(StructuredPostal.REGION);
        if (!TextUtils.isEmpty(region)) {
            dataArray[4] = region;
        } else {
            dataArray[4] = "";
        }
        dataArray[5] = contentValues.getAsString(StructuredPostal.POSTCODE);
        if (dataArray[5] == null) {
            dataArray[5] = "";
        }
        dataArray[6] = contentValues.getAsString(StructuredPostal.COUNTRY);
        if (dataArray[6] == null) {
            dataArray[6] = "";
        }

        return dataArray;
    }
    
    public static String constructNameFromElements(final int vcardType,
            final String familyName, final String middleName, final String givenName) {
        return constructNameFromElements(vcardType, familyName, middleName, givenName,
                null, null);
    }

    public static String constructNameFromElements(final int vcardType,
            final String familyName, final String middleName, final String givenName,
            final String prefix, final String suffix) {
        final StringBuilder builder = new StringBuilder();
        final String[] nameList = sortNameElements(vcardType, familyName, middleName, givenName);
        boolean first = true;
        if (!TextUtils.isEmpty(prefix)) {
            first = false;
            builder.append(prefix);
        }
        for (final String namePart : nameList) {
            if (!TextUtils.isEmpty(namePart)) {
                if (first) {
                    first = false;
                } else {
                    builder.append(' ');
                }
                builder.append(namePart);
            }
        }
        if (!TextUtils.isEmpty(suffix)) {
            if (!first) {
                builder.append(' ');
            }
            builder.append(suffix);
        }
        return builder.toString();
    }

    public static List<String> constructListFromValue(final String value,
            final boolean isV30) {
        final List<String> list = new ArrayList<String>();
        StringBuilder builder = new StringBuilder();
        int length = value.length();
        for (int i = 0; i < length; i++) {
            char ch = value.charAt(i);
            if (ch == '\\' && i < length - 1) {
                char nextCh = value.charAt(i + 1);
                final String unescapedString =
                    (isV30 ? VCardParser_V30.unescapeCharacter(nextCh) :
                        VCardParser_V21.unescapeCharacter(nextCh));
                if (unescapedString != null) {
                    builder.append(unescapedString);
                    i++;
                } else {
                    builder.append(ch);
                }
            } else if (ch == ';') {
                list.add(builder.toString());
                builder = new StringBuilder();
            } else {
                builder.append(ch);
            }
        }
        list.add(builder.toString());
        return list;
    }

    public static boolean containsOnlyPrintableAscii(String str) {
        if (TextUtils.isEmpty(str)) {
            return true;
        }

        final int length = str.length();
        final int asciiFirst = 0x20;
        final int asciiLast = 0x126;
        for (int i = 0; i < length; i = str.offsetByCodePoints(i, 1)) {
            int c = str.codePointAt(i);
            if (c < asciiFirst || asciiLast < c) {
                return false;
            }
        }
        return true;
    }

    /**
     * This is useful when checking the string should be encoded into quoted-printable
     * or not, which is required by vCard 2.1.
     * See the definition of "7bit" in vCard 2.1 spec for more information.
     */
    public static boolean containsOnlyNonCrLfPrintableAscii(String str) {
        if (TextUtils.isEmpty(str)) {
            return true;
        }

        final int length = str.length();
        final int asciiFirst = 0x20;
        final int asciiLast = 0x126;
        for (int i = 0; i < length; i = str.offsetByCodePoints(i, 1)) {
            int c = str.codePointAt(i);
            if (c < asciiFirst || asciiLast < c || c == '\n' || c == '\r') {
                return false;
            }
        }
        return true;
    }

    /**
     * This is useful since vCard 3.0 often requires the ("X-") properties and groups
     * should contain only alphabets, digits, and hyphen.
     * 
     * Note: It is already known some devices (wrongly) outputs properties with characters
     *       which should not be in the field. One example is "X-GOOGLE TALK". We accept
     *       such kind of input but must never output it unless the target is very specific
     *       to the device which is able to parse the malformed input. 
     */
    public static boolean containsOnlyAlphaDigitHyphen(String str) {
        if (TextUtils.isEmpty(str)) {
            return true;
        }

        final int lowerAlphabetFirst = 0x41;  // included ('A')
        final int lowerAlphabetLast = 0x5b;  // not included ('[')
        final int upperAlphabetFirst = 0x61;  // included ('a')
        final int upperAlphabetLast = 0x7b;  // included ('{')
        final int digitFirst = 0x30;  // included ('0')
        final int digitLast = 0x39;  // included ('9')
        final int hyphen = '-';
        final int length = str.length();
        for (int i = 0; i < length; i = str.offsetByCodePoints(i, 1)) {
            int codepoint = str.codePointAt(i);
            if (!((lowerAlphabetFirst <= codepoint && codepoint < lowerAlphabetLast) ||
                    (upperAlphabetFirst <= codepoint && codepoint < upperAlphabetLast) ||
                    (digitFirst <= codepoint && codepoint < digitLast) ||
                    (codepoint == hyphen))) {
                return false;
            }
        }
        return true;
    }
    
    static public String toHalfWidthString(String orgString) {
        if (TextUtils.isEmpty(orgString)) {
            return null;
        }
        final StringBuilder builder = new StringBuilder();
        final int length = orgString.length();
        for (int i = 0; i < length; i++) {
            // All Japanese character is able to be expressed by char.
            // Do not need to use String#codepPointAt().
            final char ch = orgString.charAt(i);
            CharSequence halfWidthText = JapaneseUtils.tryGetHalfWidthText(ch);
            if (halfWidthText != null) {
                builder.append(halfWidthText);
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
    
    private VCardUtils() {
    }
}
