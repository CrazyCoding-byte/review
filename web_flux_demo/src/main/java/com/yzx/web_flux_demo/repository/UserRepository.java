package com.yzx.web_flux_demo.repository;

import com.yzx.web_flux_demo.entity.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutorService;

/**
 * @className: User
 * @author: yzx
 * @date: 2025/11/8 9:15
 * @Version: 1.0
 * @description:
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

}
    