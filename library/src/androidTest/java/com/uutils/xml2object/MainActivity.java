package com.uutils.xml2object;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import org.xml.core.XmlReader;
import org.xml.core.XmlWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Man man1 = new Man();
        man1.name = "man1";
        man1.date = "20160130";
        man1.son = new Son();
        man1.son.name = "son1";
        man1.son.phone = "13800138000";
        man1.son.mFri = new Fri();
        man1.son.mFri.name = "fri1";
        man1.son.mFri.address = "地址";
        XmlReader xmlReader = new XmlReader();
        XmlWriter xmlWriter = new XmlWriter();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            xmlWriter.toXml(man1, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        String xmlStr = outputStream.toString();
        Log.i("xml", "" + xmlStr);
        try {
            Man man = new XmlReader().toObject(Man.class, new ByteArrayInputStream(xmlStr.getBytes()));
            Log.i("xml", "" + man);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
