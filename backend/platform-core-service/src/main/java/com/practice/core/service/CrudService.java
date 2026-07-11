package com.practice.core.service;

import com.practice.common.*;import com.practice.core.entity.*;import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.stereotype.Service;import java.util.*;

@Service
public class CrudService<T extends BaseEntity> {
    public List<T> list(JpaRepository<T,Long> dao, String keyword){ return CrudSupport.search(dao, keyword); }
    public T get(JpaRepository<T,Long> dao, Long id){ return dao.findById(id).orElseThrow(); }
    public T create(JpaRepository<T,Long> dao, T body){ return dao.save(body); }
    public T update(JpaRepository<T,Long> dao, Long id, T body){ T old=dao.findById(id).orElseThrow(); return dao.save(CrudSupport.merge(old,body)); }
    public boolean delete(JpaRepository<T,Long> dao, Long id){ dao.deleteById(id); return true; }
    public T toggle(JpaRepository<T,Long> dao, Long id){
        T old=dao.findById(id).orElseThrow(); boolean active="ENABLED".equals(old.status)||"ONLINE".equals(old.status)||"RUNNING".equals(old.status);
        old.status=active?"DISABLED":"ENABLED"; if(old instanceof Rule r) r.enabled=!active; if(old instanceof AiAgent a) a.enabled=!active; if(old instanceof TaskJob t){t.running=!active;t.status=t.running?"RUNNING":"STOPPED";} return dao.save(old);
    }
    public T status(JpaRepository<T,Long> dao, Long id, String status){ T old=dao.findById(id).orElseThrow(); old.status=status; return dao.save(old); }
}
