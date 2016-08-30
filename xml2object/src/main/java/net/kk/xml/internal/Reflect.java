package net.kk.xml.internal;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class Reflect {

    public static void set(Field field, Object object, Object value, boolean useMethod) throws IllegalAccessException {
        if (field != null) {
            accessible(field);
            String name = field.getName();
            if (useMethod) {
                try {
                    name = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
                    call(object.getClass(), object, name, value);
                    return;
                } catch (Exception e) {
                }
            }
            field.set(object, value);
        }
    }

    public static Object getFieldValue(Class<?> cls, String name, Object obj) throws NoSuchFieldException, IllegalAccessException {
        Field field = cls.getDeclaredField(name);
        if (field != null) {
            accessible(field);
            return field.get(obj);
        }
        return null;
    }

    public static Object get(Field field, Object parent) throws IllegalAccessException {
        if (field != null) {
            accessible(field);
            return field.get(parent);
        }
        return null;
    }

    public static Collection<Field> getFileds(Class<?> type) {
        Map<String, Field> result = new LinkedHashMap<String, Field>();
        do {
            for (Field field : type.getDeclaredFields()) {
                String name = field.getName();
                if (!result.containsKey(name))
                    result.put(name, field);
            }
            type = type.getSuperclass();
        } while (type != null);
        return result.values();
    }

    private static Object on(Method method, Object object, Object... args)
            throws RuntimeException {
        try {
            accessible(method);
            if (method.getReturnType() == void.class) {
                method.invoke(object, args);
                return object;
            } else {
                return method.invoke(object, args);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> getListClass(Field field) {
        if (field.getType().isAssignableFrom(List.class)) //【2】
        {
            Type fc = field.getGenericType(); // 关键的地方，如果是List类型，得到其Generic的类型
            if (fc instanceof ParameterizedType) // 【3】如果是泛型参数的类型
            {
                ParameterizedType pt = (ParameterizedType) fc;
                return (Class) pt.getActualTypeArguments()[0]; //【4】 得到泛型里的class类型对象。
            }
        }
        return Object.class;
    }

    public static Class<?>[] getMapClass(Field field) {
        if (field.getType().isAssignableFrom(Map.class)) //【2】
        {
            Type fc = field.getGenericType(); // 关键的地方，如果是List类型，得到其Generic的类型
            if (fc instanceof ParameterizedType) // 【3】如果是泛型参数的类型
            {
                ParameterizedType pt = (ParameterizedType) fc;
                return new Class[]{
                        (Class) pt.getActualTypeArguments()[0],
                        (Class) pt.getActualTypeArguments()[1]
                };
            }
        }
        return new Class[]{Object.class, Object.class};
    }

    public static Object call(Class<?> cls, Object object, String name, Object... args) throws RuntimeException {
        Class<?>[] types = types(args);
        args = reObjects(args);
        // 尝试调用方法
        try {
            Method method = exactMethod(cls, name, types);
            return on(method, object, args);
        }

        // 如果没有符合参数的方法，
        // 则匹配一个与方法名最接近的方法。
        catch (NoSuchMethodException e) {
            try {
                Method method = similarMethod(cls, name, types);
                return on(method, object, args);
            } catch (NoSuchMethodException e1) {

                throw new RuntimeException(e1);
            }
        }
    }

    /**
     * 根据方法名和方法参数得到该方法。
     */
    private static Method exactMethod(Class<?> type, String name, Class<?>[] types)
            throws NoSuchMethodException {

        // 先尝试直接调用
        try {
            return type.getMethod(name, types);
        }

        // 也许这是一个私有方法
        catch (NoSuchMethodException e) {
            do {
                try {
                    return type.getDeclaredMethod(name, types);
                } catch (NoSuchMethodException ignore) {
                }

                type = type.getSuperclass();
            } while (type != null);

            throw new NoSuchMethodException();
        }
    }

    /**
     * 给定方法名和参数，匹配一个最接近的方法
     */
    private static Method similarMethod(Class<?> type, String name, Class<?>[] types)
            throws NoSuchMethodException {
        // 对于公有方法:
        for (Method method : type.getMethods()) {
            if (isSimilarSignature(method, name, types)) {
                return method;
            }
        }
        // 对于私有方法：
        do {
            for (Method method : type.getDeclaredMethods()) {
                if (isSimilarSignature(method, name, types)) {
                    return method;
                }
            }
            type = type.getSuperclass();
        } while (type != null);
        throw new NoSuchMethodException("No similar method " + name
                + " with params " + Arrays.toString(types)
                + " could be found on type " + type + ".");
    }

    private static boolean isSimilarSignature(Method possiblyMatchingMethod,
                                              String desiredMethodName, Class<?>[] desiredParamTypes) {
        return possiblyMatchingMethod.getName().equals(desiredMethodName)
                && match(possiblyMatchingMethod.getParameterTypes(),
                desiredParamTypes);
    }

    private static boolean match(Class<?>[] declaredTypes, Class<?>[] actualTypes) {
        if (declaredTypes.length == actualTypes.length) {
            for (int i = 0; i < actualTypes.length; i++) {
                if (actualTypes[i] == NULL.class)
                    continue;

                if (wrapper(declaredTypes[i]).isAssignableFrom(
                        wrapper(actualTypes[i])))
                    continue;

                return false;
            }

            return true;
        } else {
            return false;
        }
    }

    private static Class<?>[] types(Object... values) {
        if (values == null) {
            // 空
            return new Class[0];
        }
        Class<?>[] result = new Class[values.length];
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value instanceof NULL) {
                result[i] = ((NULL) value).clsName;
            } else {
                result[i] = value == null ? Object.class : value.getClass();
            }
        }
        return result;
    }

    public static <T extends AccessibleObject> T accessible(T accessible) {
        if (accessible == null) {
            return null;
        }

        if (accessible instanceof Member) {
            Member member = (Member) accessible;

            if (Modifier.isPublic(member.getModifiers())
                    && Modifier.isPublic(member.getDeclaringClass()
                    .getModifiers())) {

                return accessible;
            }
        }

        // 默认为false,即反射时检查访问权限，
        // 设为true时不检查访问权限,可以访问private字段和方法
        if (!accessible.isAccessible()) {
            accessible.setAccessible(true);
        }

        return accessible;
    }

    public static boolean isNormal(Class<?> type) throws IllegalAccessException {
        if (type == null || type.isEnum()) {
            return true;
        }
        if (boolean.class == type || Boolean.class == type
                || int.class == type || Integer.class == type
                || long.class == type || Long.class == type
                || short.class == type || Short.class == type
                || byte.class == type || Byte.class == type
                || double.class == type || Double.class == type
                || float.class == type || Float.class == type
                || char.class == type || Character.class == type
                || String.class == type) {
            return true;
        }
        return false;
    }

    private static Object getDefault(Class<?> type) {
        if (type == null) {
            return null;
        } else if (type.isPrimitive()) {
            if (boolean.class == type) {
                return Boolean.FALSE;
            } else if (int.class == type) {
                return 0;
            } else if (long.class == type) {
                return (long) 0;
            } else if (short.class == type) {
                return (short) 0;
            } else if (byte.class == type) {
                return (byte) 0;
            } else if (double.class == type) {
                return (double) 0;
            } else if (float.class == type) {
                return (float) 0;
            } else if (char.class == type) {
                return (char) 0;
            }
        }
        return null;
    }

    public static Class<?> wrapper(Class<?> type) {
        if (type == null) {
            return Object.class;
        } else if (type.isPrimitive()) {
            if (boolean.class == type) {
                return Boolean.class;
            } else if (int.class == type) {
                return Integer.class;
            } else if (long.class == type) {
                return Long.class;
            } else if (short.class == type) {
                return Short.class;
            } else if (byte.class == type) {
                return Byte.class;
            } else if (double.class == type) {
                return Double.class;
            } else if (float.class == type) {
                return Float.class;
            } else if (char.class == type) {
                return Character.class;
            } else if (void.class == type) {
                return Void.class;
            }
        }
        return type;
    }

    public static Object wrapperValue(Class<?> type, Object object) throws IllegalAccessException {
        String value = object == null ? "" : String.valueOf(object);
        value = value.replace("\t", "").replace("\r", "").replace("\n", "");
        if (type == null) {
            return object;
        }

        if (boolean.class == type || Boolean.class == type) {
            return Boolean.parseBoolean(value);
        } else if (int.class == type || Integer.class == type) {
            if (value.trim().length() == 0) {
                return (int) 0;
            }
            return (value.startsWith("0x")) ?
                    Integer.parseInt(value.substring(2), 16) : Integer.parseInt(value);
        } else if (long.class == type || Long.class == type) {
            if (value.trim().length() == 0) {
                return (long) 0;
            }
            return (value.startsWith("0x")) ?
                    Long.parseLong(value.substring(2), 16) : Long.parseLong(value);
        } else if (short.class == type || Short.class == type) {
            if (value.trim().length() == 0) {
                return (short) 0;
            }
            return (value.startsWith("0x")) ?
                    Short.parseShort(value.substring(2), 16) : Short.parseShort(value);
        } else if (byte.class == type || Byte.class == type) {
            if (value.trim().length() == 0) {
                return (byte) 0;
            }
            return value.getBytes()[0];
        } else if (double.class == type || Double.class == type) {
            if (value.trim().length() == 0) {
                return (double) 0;
            }
            return Double.parseDouble(value);
        } else if (float.class == type || Float.class == type) {
            if (value.trim().length() == 0) {
                return (float) 0;
            }
            return Float.parseFloat(value);
        } else if (char.class == type || Character.class == type) {
            if (value.trim().length() == 0) {
                return (char) 0;
            }
            return value.toCharArray()[0];
        } else if (String.class == type) {
            return object == null ? "" : String.valueOf(object);
        } else if (type.isEnum()) {
            if (value.trim().length() == 0) {
                return null;
            }
            Object[] vals = (Object[]) Reflect.call(type, null, "values");
            for (Object o : vals) {
                //isString
                String v = String.valueOf(o);
                //value
                String i = String.valueOf(Reflect.call(o.getClass(), o, "ordinal"));
                if (value.equalsIgnoreCase(v) || value.equals(i)) {
                    return o;
                }
            }
        }
        return object;
    }

    private static Object[] reObjects(Object... args) {
        if (args != null) {
            Object[] news = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof NULL) {
                    news[i] = null;
                } else {
                    news[i] = args[i];
                }
            }
            return news;
        }
        return args;
    }

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> tClass, Class<?>[] args, Object[] objs)
            throws
            RuntimeException,
            IllegalAccessException,
            InvocationTargetException,
            InstantiationException {
//        if (tClass.isArray()) {
//            return (T) Array.newInstance(tClass.getComponentType(), 0);
//        }
//        if (tClass.isInterface() || Modifier.isAbstract(tClass.getModifiers())) {
//            if (Collection.class.isAssignableFrom(tClass)) {
//                if (args.length < 1) {
//                    throw new RuntimeException("create(Class<T>, Class<E>)");
//                }
//                return (T) createCollection(tClass);
//            }
//            if (Map.class.isAssignableFrom(tClass)) {
//                if (args.length < 2) {
//                    throw new RuntimeException("create(Class<T>, Class<K> Class<V>)");
//                }
//                return (T) createMap(tClass, args[0], args[1]);
//            }
//        }
        Constructor<T> constructor = null;
        try {
            constructor = tClass.getDeclaredConstructor(args);
        }
        // 这种情况下，构造器往往是私有的，多用于工厂方法，刻意的隐藏了构造器。
        catch (NoSuchMethodException e) {
            // private阻止不了反射的脚步:)
            int min = Integer.MAX_VALUE;
            for (Constructor<?> con : tClass.getDeclaredConstructors()) {
                if (args == null || args.length == 0) {
                    //取一个最小参数的构造，
                    if (con.getParameterTypes().length < min) {
                        min = con.getParameterTypes().length;
                        constructor = (Constructor<T>) con;
                    }
                } else {
                    if (con.getParameterTypes().length == args.length) {
                        constructor = (Constructor<T>) con;
                        break;
                    }
                }
            }
            if (constructor == null) {
                //没有默认值的参数
                throw new RuntimeException("no find default constructor " + tClass);
            }

        }
        if (constructor != null) {
            accessible(constructor);
            if (args == null || args.length == 0) {
//                System.err.print("args="+Arrays.toString(constructor.getParameterTypes()));
                objs = getDefault(constructor.getParameterTypes());
//                System.err.print("objs="+Arrays.toString(objs));
                return constructor.newInstance(objs);
            } else {
                return constructor.newInstance(objs);
            }
        }
        return null;
    }

    private static Object[] getDefault(Class<?>[] classes) {
        Object[] objects = new Object[classes.length];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = getDefault(classes[i]);
        }
        return objects;
    }

    @SuppressWarnings("unchecked")
    public static <T> Collection<T> createCollection(Class<?> pClass, Class<T> rawType) {
        if (SortedSet.class.isAssignableFrom(pClass)) {
            return new TreeSet<T>();
        } else if (EnumSet.class.isAssignableFrom(pClass)) {
            Type type = rawType.getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
                if (elementType instanceof Class) {
                    return (Collection<T>) EnumSet.noneOf((Class) elementType);
                } else {
                    throw new RuntimeException("Invalid EnumSet type: " + type.toString());
                }
            } else {
                throw new RuntimeException("Invalid EnumSet type: " + type.toString());
            }
        } else if (Set.class.isAssignableFrom(pClass)) {
            return new LinkedHashSet<T>();
        } else if (Queue.class.isAssignableFrom(pClass)) {
            return new LinkedList<T>();
        } else {
            return new ArrayList<T>();
        }
    }

    public static <K, V> Map<K, V> createMap(Class<?> rawType, Class<K> key, Class<V> value) {
        if (SortedMap.class.isAssignableFrom(rawType)) {
            return new TreeMap<K, V>();
        } else if (LinkedHashMap.class.isAssignableFrom(rawType)) {
            return new LinkedHashMap<K, V>();
        } else {
            return new HashMap<K, V>();
        }

    }

    public static class NULL {
        public NULL(Class<?> cls) {
            this.clsName = cls;
        }

        public Class<?> clsName;
    }
}