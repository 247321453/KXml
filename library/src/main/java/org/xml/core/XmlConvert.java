package org.xml.core;

import android.util.Log;

import org.xml.annotation.XmlAttribute;
import org.xml.annotation.XmlTag;
import org.xml.annotation.XmlValue;
import org.xml.bean.Tag;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Administrator on 2016/1/30.
 */
public class XmlConvert extends IXml {

    public Tag toTag(Object object, String name) throws IllegalAccessException, IOException {
        Tag tag = new Tag();
        if (object == null) return tag;
        Class<?> cls = object.getClass();
        tag.name = name == null ? getTag(object.getClass()) : name;
        if (Reflect.isNormal(cls)) {
            tag.value = toString(object);
        } else {
            writeAttributes(object, tag);
            writeText(object, tag);
            //
            Field[] fields = Reflect.getFileds(cls);
            Log.v("xml", cls + ":" + tag.name + " fileds=" + fields.length);
            for (Field field : fields) {
                if (XmlUtil.isXmlAttribute(field))
                    continue;
                if (XmlUtil.isXmlValue(field))
                    continue;
                XmlTag xmlTag = field.getAnnotation(XmlTag.class);
                String subTag;
                if (xmlTag == null) {
                    subTag = field.getName();
                } else {
                    subTag = xmlTag.value();
                }
                Reflect.accessible(field);
                Object value = field.get(object);
                //自定义类
                Class<?> fieldCls = field.getType();
                Log.v("xml", field.getType() + ":" + field.getName());
                if (Reflect.isNormal(fieldCls)) {
                    Log.v("xml", tag.name + " normal sub tag " + field.getName());
                    Tag stag = new Tag();
                    stag.name = subTag;
                    stag.value = toString(value);
                    tag.tags.add(stag);
                } else {
                    Log.v("xml", tag.name + " other sub tag " + field.getName());
                    if (fieldCls.isArray()) {
                        //数组
                        int count = Array.getLength(value);
                        for (int i = 0; i < count; i++) {
                            tag.tags.add(toTag(Array.get(value, i), subTag));
                        }
                    } else if (value instanceof Map) {
                        Object set = Reflect.call(value, "entrySet");
                        if (set instanceof Set) {
                            Set<Map.Entry<?, ?>> sets = (Set<Map.Entry<?, ?>>) set;
                            for (Map.Entry<?, ?> e : sets) {
                                Log.v("xml", "map " + e);
                                Tag mtag = new Tag();
                                mtag.name = subTag;
                                mtag.tags.add(toTag(e.getKey(), MAP_KEY));
                                mtag.tags.add(toTag(e.getValue(), MAP_VALUE));
                            }
                        }
                    } else if (value instanceof List) {
                        int count = (int) Reflect.call(value, "size");
                        for (int i = 0; i < count; i++) {
                            tag.tags.add(toTag(Reflect.call(value, "get", i), subTag));
                        }
                    } else {
                        tag.tags.add(toTag(value, subTag));
                    }
                }
            }
        }
        return tag;
    }

    public <T> T toObject(Tag tag, Class<T> tClass)
            throws
            IllegalAccessException,
            InstantiationException,
            InvocationTargetException,
            NoSuchFieldException {
        T t = Reflect.create(tClass);
        for (Map.Entry<String, String> e : tag.attributes.entrySet()) {
            setAttributes(t, e.getKey(), e.getValue());
        }
        setText(t, tag.value);

        for (Tag tag1 : tag.tags) {
            Field field = Reflect.getFiled(tClass, tag1.name);
            Class<?> fClass = field.getType();
            if (Reflect.isNormal(fClass)) {
                Reflect.set(field, t, tag1.value);
            } else if (fClass.isArray()) {
                int count = tag1.tags.size();
                Object arr = Array.newInstance(fClass.getComponentType(), count);
                for (int i = 0; i < count; i++) {
                    Array.set(arr, i, toObject(tag1.tags.get(i), fClass.getComponentType()));
                }
                Reflect.set(field, t, arr);
            } else {
                Object obj = createSubTag(t, tag1.name);
                if (obj instanceof List) {
                    Class<?> subCls = Reflect.getListClass(fClass);
                    for (Tag st : tag1.tags) {
                        Reflect.call(obj, "add", toObject(st, subCls));
                    }
                } else if (obj instanceof Map) {
                    Class<?>[] subCls = Reflect.getMapClass(obj);
                    if (subCls != null && subCls.length >= 2) {
                        for (Tag st : tag1.tags) {
                            Reflect.call(obj, "put",
                                    toObject(st.get(MAP_KEY), subCls[0]),
                                    toObject(st.get(MAP_VALUE), subCls[1]));
                        }
                    }
                } else {
                    Reflect.set(field, t, toObject(tag1, fClass));
                }
            }
        }
        return t;
    }

    private Object createSubTag(Object parent, String tag) throws NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException {
        if (parent == null || tag == null) return null;
        Class<?> cls = parent.getClass();
        Field[] fields = Reflect.getFileds(cls);
        for (Field field : fields) {
            XmlTag xmltag = field.getAnnotation(XmlTag.class);
            if (xmltag != null) {
                if (tag.equals(xmltag.value())) {
                    Log.d("xml", "create find " + tag);
                    return Reflect.create(field.getType());
                }
            }
        }
        //

        Field field = Reflect.getFiled(cls, tag);
        if (field != null) {
            return Reflect.create(field.getType());
        }
        Log.d("xml", "create no find " + tag);
        return null;
    }

    private void setText(Object object, String value) throws NoSuchFieldException, IllegalAccessException {
        if (object == null) return;
        Class<?> cls = object.getClass();
        Field[] fields = Reflect.getFileds(cls);
        for (Field field : fields) {
            XmlValue xmltag = field.getAnnotation(XmlValue.class);
            if (xmltag != null) {
                Reflect.set(field, object, value);
                return;
            }
        }
        //
        Reflect.set(Reflect.getFiled(cls, "value"), object, value);
    }

    private void setAttributes(Object object, String key, String value) throws NoSuchFieldException, IllegalAccessException {
        if (object == null || key == null) return;
        Class<?> cls = object.getClass();
        Field[] fields = Reflect.getFileds(cls);
        for (Field field : fields) {
            XmlAttribute xmltag = field.getAnnotation(XmlAttribute.class);
            if (xmltag != null) {
                if (key.equals(xmltag.value())) {
                    Reflect.set(field, object, value);
                    return;
                }
            }
        }
        //
        Reflect.set(Reflect.getFiled(cls, key), object, value);
    }

    //region write
    private void writeAttributes(Object object, Tag tag) throws IllegalAccessException {
        if (object == null || tag == null) return;
        Class<?> cls = object.getClass();
        Field[] fields = Reflect.getFileds(cls);
        for (Field field : fields) {
            if (XmlUtil.isXmlTag(field))
                continue;
            if (XmlUtil.isXmlValue(field))
                continue;
            XmlAttribute xmlAttr = field.getAnnotation(XmlAttribute.class);
            String subTag;
            if (xmlAttr == null) {
                subTag = field.getName();
            } else {
                subTag = xmlAttr.value();
            }
            Reflect.accessible(field);
            Object val = field.get(object);
            Log.v("xml", subTag + "=" + val);
            tag.attributes.put(subTag, toString(val));
        }
    }

    private void writeText(Object object, Tag tag) throws IllegalAccessException {
        if (object == null || tag == null) return;
        Class<?> cls = object.getClass();
        Field[] fields = Reflect.getFileds(cls);
        for (Field field : fields) {
            if (XmlUtil.isXmlAttribute(field))
                continue;
            if (XmlUtil.isXmlTag(field))
                continue;
            XmlValue xmlValue = field.getAnnotation(XmlValue.class);
            Reflect.accessible(field);
            Object val = field.get(object);
            if (xmlValue != null) {
                tag.value = toString(val);
            }
        }
    }
    //endregion
}
