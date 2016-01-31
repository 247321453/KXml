package org.xml.core;

import android.util.Log;
import android.util.Xml;

import org.xml.bean.Tag;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class XmlWriter extends IXml {
    static final String DEF_ENCODING = "UTF-8";

    public void toXml(Object object, OutputStream outputStream, String encoding)
            throws IOException, IllegalAccessException {
        toXml(toTag(object, null), outputStream, encoding);
    }

    public void toXml(Tag tag, OutputStream outputStream, String encoding)
            throws IOException, IllegalAccessException {
        if (outputStream == null) return;
        XmlSerializer serializer = Xml.newSerializer();
        if (encoding == null) {
            encoding = DEF_ENCODING;
        }
        serializer.setOutput(outputStream, encoding);
        serializer.startDocument(encoding, null);
        writeTag(tag, serializer);
        serializer.endDocument();
    }

    @SuppressWarnings("unchecked")
    private void writeTag(Tag tag, XmlSerializer serializer)
            throws IllegalAccessException, IOException {
        if (tag == null || serializer == null) return;
        serializer.startTag(null, tag.getName());
        for (Map.Entry<String, String> e : tag.attributes.entrySet()) {
            serializer.attribute(null, e.getKey(), e.getValue());
        }
        serializer.text(tag.getValue());
        int count = tag.size();
        for (int i = 0; i < count; i++) {
            writeTag(tag.get(i), serializer);
        }
        serializer.endTag(null, tag.getName());
    }

    public Tag toTag(Object object, String name) throws IllegalAccessException {
        Tag root = new Tag(name);
        if (object == null) return root;
        Class<?> cls = object.getClass();
        root.setClass(cls);
        if (name == null) {
            name = getTagName(cls, cls.getSimpleName());
            root.setName(name);
        }
        if (Reflect.isNormal(cls)) {
            root.setValue(toString(object));
        } else if (cls.isArray()) {
            root.addAll(array(object, name));
        } else if (object instanceof Map) {
            root.addAll(map(object, name));
        } else if (object instanceof Collection) {
            root.addAll(list(object, name));
        } else {
            writeAttributes(object, root);
            writeText(object, root);
            writeSubTag(object, root);
        }
        return root;
    }

    @SuppressWarnings("unchecked")
    private ArrayList<Tag> map(Object object, String name) throws IllegalAccessException {
        ArrayList<Tag> list = new ArrayList<>();
        if (object == null) {
            return list;
        }
        Object set = Reflect.call(object, "entrySet");
        if (set instanceof Set) {
            Set<Map.Entry<?, ?>> sets = (Set<Map.Entry<?, ?>>) set;
            for (Map.Entry<?, ?> e : sets) {
                Log.v("xml", "map " + e);
                Tag tag = new Tag(name);
                tag.add(toTag(e.getKey(), MAP_KEY));
                tag.add(toTag(e.getValue(), MAP_VALUE));
                list.add(tag);
            }
        }
        return list;
    }

    private ArrayList<Tag> array(Object object, String name) throws IllegalAccessException {
        ArrayList<Tag> list = new ArrayList<>();
        if (object != null) {
            int count = Array.getLength(object);
            for (int i = 0; i < count; i++) {
                list.add(toTag(Array.get(object, i), name));
            }
        }
        return list;
    }

    private ArrayList<Tag> list(Object object, String name) throws IllegalAccessException {
        ArrayList<Tag> list = new ArrayList<>();
        if (object != null) {
            Object[] objs = (Object[]) Reflect.call(object, "toArray");
            if (objs != null) {
                for (Object o : objs) {
                    list.add(toTag(o, name));
                }
            }
        }
        return list;
    }

    //region write
    private void writeAttributes(Object object, Tag tag) throws IllegalAccessException {
        if (object == null || tag == null) return;
        Class<?> cls = object.getClass();
        Field[] fields = Reflect.getFileds(cls);
        for (Field field : fields) {
            if (!isXmlAttribute(field))
                continue;
            String subTag = getAttributeName(field, field.getName());
            Reflect.accessible(field);
            Object val = field.get(object);
            Log.v("xml", subTag + "=" + val);
            tag.attributes.put(subTag, toString(val));
        }
    }

    private void writeSubTag(Object object, Tag tag) throws IllegalAccessException {
        if (object == null) return;
        Field[] fields = Reflect.getFileds(object.getClass());
        for (Field field : fields) {
            if (isXmlIgnore(field))
                continue;
            if (isXmlAttribute(field))
                continue;
            if (isXmlValue(field))
                continue;
            String name = getTagName(field, field.getName());
            Class<?> cls = field.getType();
            Reflect.accessible(field);
            Object val = field.get(object);
            if (val != null) {
                if (val instanceof Map) {
                    tag.addAll(map(val, name));
                } else if (val instanceof Collection) {
                    tag.addAll(list(val, name));
                } else {
                    tag.add(toTag(val, name));
                }
            }
        }
    }

    private void writeText(Object object, Tag tag) throws IllegalAccessException {
        if (object == null || tag == null) return;
        Class<?> cls = object.getClass();
        Field[] fields = Reflect.getFileds(cls);
        for (Field field : fields) {
            if (isXmlValue(field)) {
                Reflect.accessible(field);
                Object val = field.get(object);
                tag.setValue(toString(val));
                break;
            }
        }
    }
    //endregion
}
