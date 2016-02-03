package org.xml.core;

import android.util.Log;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/***
 * {@link Element } 转对象
 */
public class XmlReader extends IXml {
    protected XmlConvert mXmlConvert;

    public XmlReader() {
        mXmlConvert = new XmlConvert();
    }

    /***
     * @param inputStream 输入流
     * @param pClass      类
     * @param <T>         类型
     * @return 对象
     * @throws IllegalAccessException    异常1
     * @throws InstantiationException    异常2
     * @throws InvocationTargetException 异常3
     */
    public <T> T from(InputStream inputStream, Class<T> pClass)
            throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        Element tag = mXmlConvert.toTag(pClass, inputStream);
        if (IXml.DEBUG)
            Log.d("xml", "form " + tag);
        return any(tag, pClass, null);
    }

    @SuppressWarnings("unchecked")
    private <T> T array(List<Element> elements, Class<T> pClass, Object object, Class<?> subClass)
            throws IllegalAccessException, InstantiationException, InvocationTargetException {
        if (elements == null) {
            return null;
        }
        int count = elements.size();
        T t;
        if (object != null) {
            t = (T) object;
        } else {
            t = (T) Array.newInstance(pClass, count);
            Log.d("xml", "create array " + pClass.getName());
        }
        boolean d = XmlClassSearcher.class.isAssignableFrom(subClass);
        Class<?> sc;
        for (int i = 0; i < count; i++) {
            Element element = elements.get(i);
            if (d) {
                sc = mXmlConvert.getSubClass(subClass, element);
                if (IXml.DEBUG)
                    Log.v("xml", "child = " + sc);
            } else {
                sc = subClass;
            }
            Object o = any(element.get(i), sc, null);
            if (o != null)
                Array.set(t, i, o);
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    private <T> T map(List<Element> elements, Class<T> pClass, Object object, Class<?>[] subClass)
            throws IllegalAccessException, InstantiationException, InvocationTargetException {
        if (elements == null || subClass == null || subClass.length < 2) {
            return null;
        }
        T t;
        if (object == null) {
            t = Reflect.create(pClass, subClass);
            if (IXml.DEBUG)
                Log.d("xml", "create map " + pClass.getName());
        } else {
            t = (T) object;
        }
        if (t == null) return t;
        if (IXml.DEBUG)
            Log.v("xml", " put " + subClass[0] + "," + subClass[1] + " size=" + elements.size());
        boolean dkey = XmlClassSearcher.class.isAssignableFrom(subClass[0]);
        boolean dval = XmlClassSearcher.class.isAssignableFrom(subClass[1]);
        for (Element element : elements) {
            Class<?> kc;
            if (dkey) {
                kc = mXmlConvert.getSubClass(subClass[0], element);
            } else {
                kc = subClass[0];
            }
            Element tk = element.get(MAP_KEY);
            Object k = any(tk, kc, null);
            Class<?> vc;
            if (dval) {
                vc = mXmlConvert.getSubClass(subClass[1], element);
            } else {
                vc = subClass[0];
            }
            Element tv = element.get(MAP_VALUE);
            Object v = any(tv, vc, null);
            if (IXml.DEBUG) {
                Log.v("xml", element.getName() + " put " + (tk != null) + "=" + (tv != null));
                Log.v("xml", element.getName() + " put " + k + "=" + v);
            }
            if (k != null)
                Reflect.call(t.getClass(), t, "put", k, v);
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    private <T> T list(List<Element> elements, Class<T> pClass, Object object, Class<?> subClass)
            throws IllegalAccessException, InstantiationException, InvocationTargetException {
        if (elements == null) {
            return null;
        }
        if (IXml.DEBUG)
            Log.v("xml", "list " + subClass.getName());
        T t;
        if (object == null) {
            t = Reflect.create(pClass, subClass);
            if (IXml.DEBUG)
                Log.v("xml", "create list " + pClass.getName());
        } else {
            t = (T) object;
        }
        if (t != null) {
            //多种派生类
            boolean d = XmlClassSearcher.class.isAssignableFrom(subClass);
            for (Element element : elements) {
                Class<?> sc;
                if (d) {
                    sc = mXmlConvert.getSubClass(subClass, element);
                    if (IXml.DEBUG)
                        Log.v("xml", "child = " + sc);
                } else {
                    sc = subClass;
                }
                element.setType(sc);
                Object sub = any(element, sc, null);
                if (sub != null)
                    Reflect.call(t.getClass(), t, "add", sub);
                else {
                    Log.w("xml", element.getName() + "@" + sc.getName() + " is null");
                }
            }
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    private <T> T any(Element element, Class<T> pClass, Object object)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        if (element == null) {
            return null;
        } else if (element.getType() == null) {
            return null;
        }
        if (Reflect.isNormal(pClass)) {
            if (IXml.DEBUG)
                Log.v("xml", "create normal " + element.getName() + " " + pClass);
            if (object == null) {
                try {
                    object = Reflect.wrapper(pClass, element.getText());
                } catch (Throwable e) {
                }
            }
            return (T) object;
        } else {
            if (IXml.DEBUG)
                Log.v("xml", "create other " + element.getName() + " " + pClass);
            return object(element, pClass, object);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T object(Element element, Class<T> pClass, Object parent) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        if (Reflect.isNormal(pClass)) {
            if (IXml.DEBUG)
                Log.v("xml", "create normal " + element.getName() + " " + pClass);
            return (T) Reflect.wrapper(pClass, element.getText());
        }
        T t = (parent == null) ? Reflect.create(pClass) : (T) parent;
        //attr
        for (Map.Entry<String, String> e : element.getAttributes().entrySet()) {
            setAttribute(t, e.getKey(), e.getValue());
        }
        setText(t, element.getText());
        int count = element.size();
        List<String> oldtags = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Element el = element.get(i);
            String name = el.getName();
            if (oldtags.contains(name))
                continue;
            Field field = Reflect.getFiled(pClass, name);
            if (field == null) {
                Log.w("xml", "no find field " + name);
                continue;
            }
            oldtags.add(name);
            Class<?> cls = field.getType();
            Object val = Reflect.get(field, t);
            if (cls.isArray()) {
                Reflect.set(field, t, array(element.getElementList(name), cls, val, getArrayClass(field)));
            } else if (Collection.class.isAssignableFrom(cls)) {
                Reflect.set(field, t, list(element.getElementList(name), cls, val, getListClass(field)));
            } else if (Map.class.isAssignableFrom(cls)) {
                Reflect.set(field, t, map(element.getElementList(name), cls, val, getMapClass(field)));
            } else {
                Reflect.set(field, t, any(el, cls, val));
            }
        }
        return t;
    }

    private void setAttribute(Object object, String tag, Object value)
            throws IllegalAccessException {
        if (object == null || tag == null) return;
        Field[] fields = Reflect.getFileds(object.getClass());
        for (Field field : fields) {
            if (isXmlAttribute(field)) {
                String name = getAttributeName(field, field.getName());
                if (tag.equals(name)) {
                    Reflect.set(field, object, value);
                    break;
                }
            }
        }
    }

    private void setText(Object object, Object value)
            throws IllegalAccessException {
        if (object == null) return;
        Field[] fields = Reflect.getFileds(object.getClass());
        for (Field field : fields) {
            if (isXmlValue(field)) {
                Reflect.set(field, object, value);
                break;
            }
        }
    }
}
