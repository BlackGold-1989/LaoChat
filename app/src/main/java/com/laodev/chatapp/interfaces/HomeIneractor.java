package com.laodev.chatapp.interfaces;

import com.laodev.chatapp.models.Contact;
import com.laodev.chatapp.models.User;

import java.util.ArrayList;

/**
 * Created by a_man on 01-01-2018.
 */

public interface HomeIneractor {
    User getUserMe();

    ArrayList<Contact> getLocalContacts();

}
