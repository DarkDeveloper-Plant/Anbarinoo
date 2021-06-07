package ir.darkdeveloper.anbarinoo.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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

import ir.darkdeveloper.anbarinoo.exception.BadRequestException;
import ir.darkdeveloper.anbarinoo.exception.DataExistsException;
import ir.darkdeveloper.anbarinoo.exception.ForbiddenException;
import ir.darkdeveloper.anbarinoo.exception.InternalServerException;
import ir.darkdeveloper.anbarinoo.model.Authority;
import ir.darkdeveloper.anbarinoo.model.UserModel;
import ir.darkdeveloper.anbarinoo.model.VerificationModel;
import ir.darkdeveloper.anbarinoo.repository.UserRepo;
import ir.darkdeveloper.anbarinoo.security.jwt.JwtAuth;
import ir.darkdeveloper.anbarinoo.util.AdminUserProperties;
import ir.darkdeveloper.anbarinoo.util.UserUtils;
import lombok.AllArgsConstructor;

@Service("userService")
@AllArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepo repo;
    private final UserUtils userUtils;
    private final AdminUserProperties adminUser;
    private final VerificationService verificationService;

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
            } catch (IOException e) {
                throw new InternalServerException(e.getLocalizedMessage());
            }
        }
        throw new ForbiddenException("You are not allowed to update user!");
    }

    @Transactional
    @PreAuthorize("authentication.name == this.getAdminUsername() || #user.getEmail() == authentication.name")
    public ResponseEntity<?> deleteUser(UserModel user) {
        try {
            UserModel model = repo.findUserById(user.getId());
            userUtils.deleteUser(model);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (IOException e) {
            throw new InternalServerException(e.getLocalizedMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('OP_ACCESS_ADMIN','OP_ACCESS_USER')")
    public Page<UserModel> allUsers(Pageable pageable) {
        return repo.findAll(pageable);
    }

    public ResponseEntity<?> loginUser(JwtAuth model, HttpServletResponse response) {

        if (model.getUsername().equals(adminUser.getUsername()))
            userUtils.authenticateUser(model, null, null, response);
        else
            userUtils.authenticateUser(model, userUtils.getUserIdByUsernameOrEmail(model.getUsername()), null,
                    response);
        return new ResponseEntity<>(repo.findByEmailOrUsername(model.getUsername()), HttpStatus.OK);

    }

    public ResponseEntity<?> signUpUser(UserModel model, HttpServletResponse response) throws IOException, Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getName().equals("anonymousUser") || auth.getAuthorities().contains(Authority.OP_ACCESS_ADMIN)
                || !auth.getName().equals(model.getEmail())) {
            try {
                userUtils.signupValidation(model, response);
                return new ResponseEntity<>(repo.findByEmailOrUsername(model.getUsername()), HttpStatus.OK);
            } catch (DataIntegrityViolationException e) {
                throw new DataExistsException("User exists!");
            }
        }
        throw new ForbiddenException("You are not allowed to signup a user!");
    }

    @PreAuthorize("hasAnyAuthority('OP_ACCESS_ADMIN','OP_ACCESS_USER')")
    public UserModel getUserInfo(UserModel model) {
        return repo.findUserById(model.getId());
    }

    public ResponseEntity<?> verifyUserEmail(String token) {

        Optional<VerificationModel> model = verificationService.findByToken(token);

        if (model.isPresent())
            if (model.get().getExpiresAt().isBefore(LocalDateTime.now())) {
                model.get().setVerifiedAt(LocalDateTime.now());
                repo.trueEnabledById(model.get().getUser().getId());
                verificationService.saveToken(model.get());
                return new ResponseEntity<>("Email Successfully verified", HttpStatus.OK);
            } else
                throw new BadRequestException("Link is expired. try logging in again");

        throw new InternalServerException("Link does not exists");
    }

}
