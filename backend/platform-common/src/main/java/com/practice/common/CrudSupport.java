package com.practice.common;

import org.springframework.data.jpa.repository.JpaRepository;
import java.lang.reflect.Field;
import java.util.List;

public final class CrudSupport {
    private CrudSupport() {}
    public static <T> List<T> search(JpaRepository<T,Long> repo, String keyword) { List<T> all=repo.findAll(); if (keyword==null || keyword.isBlank()) return all; String kw=keyword.toLowerCase(); return all.stream().filter(x -> text(x).contains(kw)).toList(); }
    public static String text(Object x){ StringBuilder sb=new StringBuilder(); for(Class<?> c=x.getClass(); c!=null; c=c.getSuperclass()) for(Field f:c.getDeclaredFields()) try{f.setAccessible(true); Object v=f.get(x); if(v!=null) sb.append(v).append(' ');}catch(Exception ignored){} return sb.toString().toLowerCase(); }
    public static <T> T merge(T old, T body){ for(Class<?> c=body.getClass(); c!=null; c=c.getSuperclass()) for(Field f:c.getDeclaredFields()) try{ f.setAccessible(true); if("id".equals(f.getName())||"createdAt".equals(f.getName())) continue; Object v=f.get(body); if(v!=null) f.set(old,v); }catch(Exception ignored){} return old; }
}
