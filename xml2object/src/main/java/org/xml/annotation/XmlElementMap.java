/*
 * Copyright (c) 2013. wyouflf (wyouflf@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xml.annotation;

import org.xml.core.KXml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 元素，不支持接口，Void类型，除非接口在默认构造函数初始化
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface XmlElementMap {
    String value();
    /**
     *
     * @return Map的key的元素名
     */
    String keyName() default KXml.MAP_KEY_NAME;

    /**
     *
     * @return Map的value的元素名
     */
    String valueName() default KXml.MAP_VALUE_NAME;
    /**
     * @return Map的value元素类型
     */
    Class<?> valueType();

    /**
     * @return Map的key元素类型
     */
    Class<?> keyType();
}