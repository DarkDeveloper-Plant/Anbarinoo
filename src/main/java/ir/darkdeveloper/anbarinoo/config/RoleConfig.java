package ir.darkdeveloper.anbarinoo.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import ir.darkdeveloper.anbarinoo.model.Authority;
import ir.darkdeveloper.anbarinoo.model.UserRoles;
import ir.darkdeveloper.anbarinoo.service.UserRolesService;

@Configuration
public class RoleConfig {

    private final UserRolesService service;

    @Autowired
    public RoleConfig(UserRolesService service) {
        this.service = service;
    }

    private void createDefaultRole() {
        if (!service.exists("USER")) {
            List<Authority> authorities = new ArrayList<>();
            authorities.addAll(List.of(Authority.OP_EDIT_USER, Authority.OP_ACCESS_USER, Authority.OP_DELETE_USER));
            service.saveRole(new UserRoles(1L, "USER", authorities));
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() {
        createDefaultRole();
    }
}