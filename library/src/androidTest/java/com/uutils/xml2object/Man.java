package com.uutils.xml2object;

import org.xml.annotation.XmlAttribute;
import org.xml.annotation.XmlTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2016/1/30.
 */
@XmlTag("man")
public class Man {
    public Man() {
        maps = new HashMap<>();
    }

    @XmlAttribute("name1")
    String name;

    @XmlAttribute("date1")
    String date;

    @XmlTag("son1")
    List<Son> sons;

    @XmlTag("maps")
    final Map<String, Integer> maps;

    @Override
    public String toString() {
        return "Man{" +
                " name='" + name + '\'' +
                ", son=" + sons +
                '}';
    }
}
