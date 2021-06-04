package ir.darkdeveloper.anbarinoo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import ir.darkdeveloper.anbarinoo.model.UserModel;
import ir.darkdeveloper.anbarinoo.repository.UserRepo;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {
    
    private final UserRepo repo;

    @Autowired
    public OAuth2UserService(UserRepo repo) {
        this.repo = repo;
    }


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        UserModel user = repo.findByEmailOrUsername(oAuth2User.getAttribute("email"));
        if(user == null){
            user = new UserModel();
            user.setEmail(oAuth2User.getAttribute("email"));
        }
        user.setEnabled(oAuth2User.getAttribute("email_verified"));
        user.setShopImage(oAuth2User.getAttribute("picture"));
        user = repo.save(user);
        return user;
    }

}
