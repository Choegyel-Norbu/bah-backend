package com.attirehub.returns.repository;

import com.attirehub.returns.entity.OrderReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderReturnRequestRepository extends JpaRepository<OrderReturnRequest, Long> {

    @EntityGraph(attributePaths = {"order"})
    @Query("SELECT r FROM OrderReturnRequest r ORDER BY r.createdAt DESC")
    Page<OrderReturnRequest> findPage(Pageable pageable);
}
