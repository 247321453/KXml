//package org.xml.core;
//
//import android.util.Log;
//
//import org.xml.annotation.XmlAttribute;
//import org.xml.annotation.XmlTag;
//import org.xml.annotation.XmlValue;
//import org.xml.bean.Root;
//import org.xml.bean.Tag;
//
//import java.lang.reflect.Array;
//import java.lang.reflect.Field;
//import java.lang.reflect.InvocationTargetException;
//import java.util.Collection;
//import java.util.List;
//import java.util.Map;
//
///**
// * Created by Administrator on 2016/1/30.
// */
//public class XmlConvert extends IXml {
//
//    @SuppressWarnings("unchecked")
//    public <T> T toObject(Root tag, Class<T> cls)
//            throws
//            IllegalAccessException,
//            InstantiationException,
//            InvocationTargetException,
//            NoSuchFieldException {
//        T object = null;
//        String name = getTagName(cls);
//        if (cls.isArray()) {
//            object = (T) array(cls, tag.getList(name));
//            return object;
//        }
//        object = Reflect.create(cls);
//        if (object instanceof Map) {
//            map(object, tag.getList(name));
//        } else if (object instanceof Collection) {
//            list(object, tag.getList(name));
//        } else {
//            any(object, tag.get(name));
//        }
//        return object;
//    }
//
//    private Object array(Class<?> pClass, List<Tag> tags) throws IllegalAccessException, InstantiationException, InvocationTargetException {
//        Class<?> arrCls = pClass.getComponentType();
//        Object object = Array.newInstance(arrCls, tags.size());
//        for (int i = 0; i < tags.size(); i++) {
//            Object obj = Reflect.create(arrCls);
//            Array.set(object, i, any(obj, tags.get(i)));
//        }
//        return object;
//    }
//
//    private Object map(Object obj, List<Tag> tags) {
//        return null;
//    }
//
//    private Object list(Object obj, List<Tag> tags) {
//        return null;
//    }
//
//    private Object any(Object obj, Tag tag) {
//        return null;
//    }
//
////        for (Map.Entry<String, String> e : tag.attributes.entrySet()) {
////            setAttributes(t, e.getKey(), e.getValue());
////        }
////        setText(t, tag.value);
////
////        for (Tag tag1 : tag.tags) {
////            Field field = Reflect.getFiled(tClass, tag1.name);
////            Class<?> fClass = field.getType();
////            if (Reflect.isNormal(fClass)) {
////                Reflect.set(field, t, tag1.value);
////            } else if (fClass.isArray()) {
////                int count = tag1.tags.size();
////                Object arr = Array.newInstance(fClass.getComponentType(), count);
////                for (int i = 0; i < count; i++) {
////                    Array.set(arr, i, toObject(tag1.tags.get(i), fClass.getComponentType()));
////                }
////                Reflect.set(field, t, arr);
////            } else {
////                Object obj = createSubTag(t, tag1.name);
////                if (obj instanceof List) {
////                    Class<?> subCls = Reflect.getListClass(fClass);
////                    for (Tag st : tag1.tags) {
////                        Reflect.call(obj, "add", toObject(st, subCls));
////                    }
////                } else if (obj instanceof Map) {
////                    Class<?>[] subCls = Reflect.getMapKeyAndValueTypes(obj);
////                    if (subCls != null && subCls.length >= 2) {
////                        for (Tag st : tag1.tags) {
////                            Reflect.call(obj, "put",
////                                    toObject(st.get(MAP_KEY), subCls[0]),
////                                    toObject(st.get(MAP_VALUE), subCls[1]));
////                        }
////                    }
////                } else {
////                    Reflect.set(field, t, toObject(tag1, fClass));
////                }
////            }
////        }
////        return t;
////    }
//
//    private Object createSubTag(Object parent, String tag) throws NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException {
//        if (parent == null || tag == null) return null;
//        Class<?> cls = parent.getClass();
//        Field[] fields = Reflect.getFileds(cls);
//        for (Field field : fields) {
//            XmlTag xmltag = field.getAnnotation(XmlTag.class);
//            if (xmltag != null) {
//                if (tag.equals(xmltag.value())) {
//                    Log.d("xml", "create find " + tag);
//                    return Reflect.create(field.getType());
//                }
//            }
//        }
//        //
//
//        Field field = Reflect.getFiled(cls, tag);
//        if (field != null) {
//            return Reflect.create(field.getType());
//        }
//        Log.d("xml", "create no find " + tag);
//        return null;
//    }
//
//    private void setText(Object object, String value) throws NoSuchFieldException, IllegalAccessException {
//        if (object == null) return;
//        Class<?> cls = object.getClass();
//        Field[] fields = Reflect.getFileds(cls);
//        for (Field field : fields) {
//            XmlValue xmltag = field.getAnnotation(XmlValue.class);
//            if (xmltag != null) {
//                Reflect.set(field, object, value);
//                return;
//            }
//        }
//        //
//        Reflect.set(Reflect.getFiled(cls, "value"), object, value);
//    }
//
//    private void setAttributes(Object object, String key, String value) throws NoSuchFieldException, IllegalAccessException {
//        if (object == null || key == null) return;
//        Class<?> cls = object.getClass();
//        Field[] fields = Reflect.getFileds(cls);
//        for (Field field : fields) {
//            XmlAttribute xmltag = field.getAnnotation(XmlAttribute.class);
//            if (xmltag != null) {
//                if (key.equals(xmltag.value())) {
//                    Reflect.set(field, object, value);
//                    return;
//                }
//            }
//        }
//        //
//        Reflect.set(Reflect.getFiled(cls, key), object, value);
//    }
//
//}
