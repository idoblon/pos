package com.springboot.POS.service;

import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.modal.ShiftReport;
import com.springboot.POS.payload.dto.ShiftReportDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface ShiftReportService {
    ShiftReportDTO startShift() throws Exception;
    ShiftReportDTO endShift(Long shiftReportId, LocalDateTime shiftEnd) throws Exception;
    ShiftReportDTO getShiftReportById(Long id) throws Exception;
    List<ShiftReportDTO> getAllShiftReports();
    List<ShiftReportDTO> getShiftReportByBranchId(Long branchId);
    List<ShiftReportDTO> getShiftReportByCashierId(Long cashierId);
    ShiftReportDTO getCurrentShiftProgress(Long cashierId) throws Exception;
    ShiftReportDTO getShiftByCashierAndDate(Long cashierId, LocalDateTime date) throws Exception;

}
