package com.practice.core.service;

import com.practice.common.*;import com.practice.core.dao.*;import com.practice.core.entity.*;import jakarta.servlet.http.HttpServletRequest;import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.stereotype.Service;import org.springframework.web.context.request.*;import java.util.*;

@Service
public class CrudService<T extends BaseEntity> {
    private final OperationLogDao opLogs;
    public CrudService(OperationLogDao opLogs){this.opLogs=opLogs;}
    public List<T> list(JpaRepository<T,Long> dao, String keyword){ return CrudSupport.search(dao, keyword); }
    public T get(JpaRepository<T,Long> dao, Long id){ return dao.findById(id).orElseThrow(); }
    public T create(JpaRepository<T,Long> dao, T body){ T saved=dao.save(body); log(saved,"新增"); return saved; }
    public T update(JpaRepository<T,Long> dao, Long id, T body){ T old=dao.findById(id).orElseThrow(); T saved=dao.save(CrudSupport.merge(old,body)); log(saved,"更新"); return saved; }
    public boolean delete(JpaRepository<T,Long> dao, Long id){ T old=dao.findById(id).orElse(null); dao.deleteById(id); log(old,"删除"); return true; }
    public T toggle(JpaRepository<T,Long> dao, Long id){
        T old=dao.findById(id).orElseThrow(); boolean active="ENABLED".equals(old.status)||"ONLINE".equals(old.status)||"RUNNING".equals(old.status);
        old.status=active?"DISABLED":"ENABLED"; if(old instanceof Rule r) r.enabled=!active; if(old instanceof AiAgent a) a.enabled=!active; if(old instanceof TaskJob t){t.running=!active;t.status=t.running?"RUNNING":"STOPPED";} T saved=dao.save(old); log(saved,active?"停用":"启用"); return saved;
    }
    public T status(JpaRepository<T,Long> dao, Long id, String status){ T old=dao.findById(id).orElseThrow(); old.status=status; T saved=dao.save(old); log(saved,"状态变更"); return saved; }
    void log(T body,String action){if(body==null||body instanceof OperationLog)return;OperationLog l=new OperationLog();l.name=moduleName(body)+"-"+action;l.moduleName=moduleName(body);l.action=action;l.operator=operator();l.detail=Optional.ofNullable(body.name).filter(x->!x.isBlank()).orElse(Optional.ofNullable(body.code).orElse(String.valueOf(body.id)));opLogs.save(l);}
    String operator(){RequestAttributes attrs=RequestContextHolder.getRequestAttributes();if(attrs instanceof ServletRequestAttributes s){HttpServletRequest r=s.getRequest();String user=Optional.ofNullable(r.getHeader("X-User-Name")).orElse("").trim();if(!user.isBlank())return user;}return"admin";}
    String moduleName(T body){if(body instanceof DeviceAttribute)return"设备属性";if(body instanceof ProductCategory)return"产品分类";if(body instanceof Product)return"产品管理";if(body instanceof DeviceGroup)return"设备分组";if(body instanceof Device)return"设备管理";if(body instanceof NetworkService)return"网络服务";if(body instanceof ParseScript)return"解析脚本";if(body instanceof Rule)return"规则设计";if(body instanceof RuleAudit)return"规则审计";if(body instanceof TaskJob)return"任务中心";if(body instanceof SysUser)return"用户管理";if(body instanceof Role)return"角色管理";if(body instanceof Firmware)return"固件管理";if(body instanceof AiAgent)return"AI智能体";return body.getClass().getSimpleName();}
}
