package ir.darkdeveloper.anbarinoo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ir.darkdeveloper.anbarinoo.model.UserModel;

@Repository
public interface UserRepo extends PagingAndSortingRepository<UserModel, Long> {

    @Query("SELECT model FROM UserModel model WHERE model.email = :username OR model.userName = :username")
    UserModel findByEmailOrUsername(@Param("username") String username);

    UserModel findUserById(Long id);

    Page<UserModel> findAll(Pageable pageable);

    @Query("SELECT model.id FROM UserModel model WHERE model.email = :username OR model.userName = :username")
    Long findUserIdByUsername(@Param("username") String username);

    @Modifying
    @Query("UPDATE UserModel m SET m.enabled = true WHERE m.id = :id")
    void trueEnabledById(@Param("id") Long id);
}
