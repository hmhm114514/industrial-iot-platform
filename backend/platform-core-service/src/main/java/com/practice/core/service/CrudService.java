package com.practice.core.service;

import com.practice.common.*;import com.practice.core.entity.*;import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.stereotype.Service;import java.util.*;

@Service
public class CrudService<T extends BaseEntity> {
    public List<T> list(JpaRepository<T,Long> repo, String keyword){ return CrudSupport.search(repo, keyword); }
    public T get(JpaRepository<T,Long> repo, Long id){ return repo.findById(id).orElseThrow(); }
    public T create(JpaRepository<T,Long> repo, T body){ return repo.save(body); }
    public T update(JpaRepository<T,Long> repo, Long id, T body){ T old=repo.findById(id).orElseThrow(); return repo.save(CrudSupport.merge(old,body)); }
    public boolean delete(JpaRepository<T,Long> repo, Long id){ repo.deleteById(id); return true; }
    public T toggle(JpaRepository<T,Long> repo, Long id){
        T old=repo.findById(id).orElseThrow(); boolean active="ENABLED".equals(old.status)||"ONLINE".equals(old.status)||"RUNNING".equals(old.status);
        old.status=active?"DISABLED":"ENABLED"; if(old instanceof Rule r) r.enabled=!active; if(old instanceof AiAgent a) a.enabled=!active; if(old instanceof TaskJob t){t.running=!active;t.status=t.running?"RUNNING":"STOPPED";} return repo.save(old);
    }
    public T status(JpaRepository<T,Long> repo, Long id, String status){ T old=repo.findById(id).orElseThrow(); old.status=status; return repo.save(old); }
}
