package ir.darkdeveloper.anbarinoo.service;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ir.darkdeveloper.anbarinoo.model.RefreshModel;
import ir.darkdeveloper.anbarinoo.repository.RefreshRepo;

import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshService {

    private final RefreshRepo repo;

    @Transactional
    public void saveToken(RefreshModel model) {
        repo.save(model);
    }

    @Transactional
    public void deleteTokenByUserId(Long id) {
        repo.deleteTokenByUserId(id);
    }

    public RefreshModel getRefreshByUserId(Long id) {
        return repo.getRefreshByUserId(id);
    }

    public Long getIdByUserId(Long adminId) {
        return repo.getIdByUserId(adminId);
    }

}
