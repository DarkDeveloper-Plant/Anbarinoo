package ir.darkdeveloper.anbarinoo.service;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import ir.darkdeveloper.anbarinoo.model.Authority;
import ir.darkdeveloper.anbarinoo.model.UserModel;
import ir.darkdeveloper.anbarinoo.repository.UserRepo;
import ir.darkdeveloper.anbarinoo.security.jwt.JwtAuth;
import ir.darkdeveloper.anbarinoo.util.AdminUserProperties;
import ir.darkdeveloper.anbarinoo.util.UserUtils;

@Service("userService")
public class UserService implements UserDetailsService {

    private final UserRepo repo;
    private final UserUtils userUtils;
    private AdminUserProperties adminUser;

    @Autowired
    public UserService(UserRepo repo, UserRolesService roleService, UserUtils userUtils, AdminUserProperties adminUser) {
        this.repo = repo;
        this.userUtils = userUtils;
        this.adminUser = adminUser;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userUtils.loadUserByUsername(username);
    }

    @Transactional
    public UserModel updateUser(UserModel model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!auth.getName().equals("anonymousUser") || auth.getAuthorities().contains(Authority.OP_ACCESS_ADMIN)
                || auth.getName().equals(model.getEmail())) {
            try {
                userUtils.validateUserData(model);
                return repo.save(model);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Transactional
    @PreAuthorize("authentication.name == this.getAdminUsername() || #user.getEmail() == authentication.name")
    public ResponseEntity<?> deleteUser(UserModel user) {
        try {
            UserModel model = repo.findUserById(user.getId());
            userUtils.deleteUser(model);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasAnyAuthority('OP_ACCESS_ADMIN','OP_ACCESS_USER')")
    public Page<UserModel> allUsers(Pageable pageable) {
        return repo.findAll(pageable);
    }

    public ResponseEntity<?> loginUser(JwtAuth model, HttpServletResponse response) {

        try {
            if (model.getUsername().equals(adminUser.getUsername()))
                userUtils.authenticateUser(model, null, null, response);
            else
                userUtils.authenticateUser(model, userUtils.getUserIdByUsernameOrEmail(model.getUsername()), null,
                        response);

            return new ResponseEntity<>(repo.findByEmailOrUsername(model.getUsername()), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<?> signUpUser(UserModel model, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getName().equals("anonymousUser") || auth.getAuthorities().contains(Authority.OP_ACCESS_ADMIN)
                || !auth.getName().equals(model.getEmail())) {
            try {

                if (model.getUserName() != null && model.getUserName().equals(adminUser.getUsername()))
                    return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);

                String rawPass = model.getPassword();
                userUtils.validateUserData(model);
                repo.save(model);
                JwtAuth jwtAuth = new JwtAuth();
                jwtAuth.setUsername(model.getEmail());
                jwtAuth.setPassword(model.getPassword());
                userUtils.authenticateUser(jwtAuth, model.getId(), rawPass, response);
                return new ResponseEntity<>(repo.findByEmailOrUsername(model.getUsername()), HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
            }
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @PreAuthorize("hasAnyAuthority('OP_ACCESS_ADMIN','OP_ACCESS_USER')")
    public UserModel getUserInfo(UserModel model) {
        return repo.findUserById(model.getId());
    }

}
