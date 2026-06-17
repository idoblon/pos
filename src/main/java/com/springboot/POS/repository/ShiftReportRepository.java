package com.springboot.POS.repository;

import com.springboot.POS.modal.ShiftReport;
import com.springboot.POS.modal.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShiftReportRepository extends JpaRepository<ShiftReport, Long> {

    List<ShiftReport> findByCashierId(Long id);

    @Query("""
            SELECT DISTINCT sr FROM ShiftReport sr
            LEFT JOIN sr.branch shiftBranch
            LEFT JOIN sr.cashier cashier
            LEFT JOIN cashier.branch cashierBranch
            WHERE shiftBranch.id = :branchId
               OR cashierBranch.id = :branchId
            """)
    List<ShiftReport> findByBranchIdIncludingCashierBranch(@Param("branchId") Long branchId);


    Optional<ShiftReport> findTopByCashierAndShiftEndIsNullOrderByShiftStartDesc(
            User cashier
    );

    Optional<ShiftReport> findByCashierAndShiftStartBetween(
            User cashier, LocalDateTime start, LocalDateTime end
    );
}
