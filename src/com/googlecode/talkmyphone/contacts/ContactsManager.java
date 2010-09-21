package com.googlecode.talkmyphone.contacts;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.People;

import com.googlecode.talkmyphone.Tools;
import com.googlecode.talkmyphone.XmppService;

public class ContactsManager {

    // Contact searching
    private final static String cellPhonePattern = "0[67]\\d{8}";
    private final static String internationalPrefix = "+33";

    /**
     * Tries to get the contact display name of the specified phone number.
     * If not found, returns the argument.
     */
    public static String getContactName (String phoneNumber) {
        String res = phoneNumber;
        ContentResolver resolver = XmppService.getInstance().getContentResolver();
        String[] projection = new String[] {
                Contacts.Phones.DISPLAY_NAME,
                Contacts.Phones.NUMBER };
        Uri contactUri = Uri.withAppendedPath(Contacts.Phones.CONTENT_FILTER_URL, Uri.encode(phoneNumber));
        Cursor c = resolver.query(contactUri, projection, null, null, null);
        if (c.moveToFirst()) {
            String name = c.getString(c.getColumnIndex(Contacts.Phones.DISPLAY_NAME));
            res = name;
        }
        return res;
    }

    /**
     * Returns a ArrayList of <Contact> where the names/company match the argument
     */
    public static ArrayList<Contact> getMatchingContacts(String searchedName) {
        ArrayList<Contact> res = new ArrayList<Contact>();
        if (!searchedName.equals(""))
        {
            ContentResolver resolver = XmppService.getInstance().getContentResolver();
            String[] projection = new String[] {
                    Contacts.People._ID,
                    Contacts.People.NAME
                    };
            Uri contactUri = Uri.withAppendedPath(People.CONTENT_FILTER_URI, Uri.encode(searchedName));
            Cursor c = resolver.query(contactUri, projection, null, null, null);
            for (boolean hasData = c.moveToFirst() ; hasData ; hasData = c.moveToNext()) {
                Long id = Tools.getLong(c, People._ID);
                if (null != id) {
                    String contactName = Tools.getString(c, People.NAME);
                    if(null != contactName) {
                        Contact contact = new Contact();
                        contact.id = id;
                        contact.name = contactName;
                        // todo add address
                        res.add(contact);
                    }
                }
            }
            c.close();
        }
        return res;
    }

    /**
     * Returns a ArrayList < Phone >
     * with all matching phones for the argument
     */
    public static ArrayList<Phone> getPhones(String searchedText) {
        ArrayList<Phone> res = new ArrayList<Phone>();
        if (isCellPhoneNumber(searchedText)) {
            Phone phone = new Phone();
            phone.number = searchedText;
            phone.cleanNumber = cleanPhoneNumber(phone.number);
            phone.contactName = getContactName(searchedText);
            phone.isCellPhoneNumber = true;
            phone.type = Contacts.Phones.TYPE_MOBILE;

            res.add(phone);
        } else {
            // get the matching contacts, dictionary of < id, names >
            ArrayList<Contact> contacts = getMatchingContacts(searchedText);
            if (contacts.size() > 0) {
                ContentResolver resolver = XmppService.getInstance().getContentResolver();

                for (Contact contact : contacts) {
                    Uri personUri = ContentUris.withAppendedId(People.CONTENT_URI, contact.id);
                    Uri phonesUri = Uri.withAppendedPath(personUri, People.Phones.CONTENT_DIRECTORY);
                    String[] proj = new String[] {Contacts.Phones.NUMBER, Contacts.Phones.LABEL, Contacts.Phones.TYPE};
                    Cursor c = resolver.query(phonesUri, proj, null, null, null);

                    for (boolean hasData = c.moveToFirst() ; hasData ; hasData = c.moveToNext()) {
                        String number = Tools.getString(c,Contacts.Phones.NUMBER);

                        String label = Tools.getString(c,Contacts.Phones.LABEL);
                        int type = Tools.getLong(c,Contacts.Phones.TYPE).intValue();

                        if (label == null || label.compareTo("") != 0) {
                            label = Contacts.Phones.getDisplayLabel(XmppService.getInstance().getBaseContext(), type, "").toString();
                        }

                        Phone phone = new Phone();
                        phone.number = number;
                        phone.cleanNumber = cleanPhoneNumber(phone.number);
                        phone.contactName = getContactName(contact.name);
                        phone.isCellPhoneNumber = isCellPhoneNumber(phone.number);
                        phone.label = label;
                        phone.type = type;

                        res.add(phone);
                    }
                }
            }
        }
        return res;
    }

    /**
     * Returns a ArrayList < Phone >
     * with all matching mobile phone for the argument
     */
    public static ArrayList<Phone> getMobilePhones(String searchedText) {
        ArrayList<Phone> res = new ArrayList<Phone>();
        ArrayList<Phone> phones = getPhones(searchedText);

        for (Phone phone : phones) {
            if (phone.isCellPhoneNumber) {
                res.add(phone);
            }
        }

        // manage not french cell phones
        if (res.size() == 0) {
            for (Phone phone : phones) {
                if (phone.type == Contacts.Phones.TYPE_MOBILE) {
                    res.add(phone);
                }
            }
        }

        return res;
    }

    public static String cleanPhoneNumber(String number) {
        return number.replace("(", "")
                     .replace(")", "")
                     .replace(" ", "")
                     .replace(internationalPrefix, "0");
    }

    public static boolean isCellPhoneNumber(String number) {
        return cleanPhoneNumber(number).matches(cellPhonePattern);
    }
}
