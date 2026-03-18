package com.billpoint.backend.controller;

import com.billpoint.backend.dto.MessageResponse;
import com.billpoint.backend.model.Attendance;
import com.billpoint.backend.model.Staff;
import com.billpoint.backend.repository.AttendanceRepository;
import com.billpoint.backend.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/attendance")
@PreAuthorize("hasAnyRole('STAFF', 'SHOP_OWNER')")
public class AttendanceController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StaffRepository staffRepository;

    @PostMapping("/mark")
    public ResponseEntity<?> markAttendance(@RequestBody Attendance attendance) {
        if (attendance.getStaff() == null || attendance.getStaff().getId() == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Staff info missing"));
        }

        Staff staff = staffRepository.findById(attendance.getStaff().getId())
                .orElseThrow(() -> new RuntimeException("Error: Staff not found"));

        // Check for duplicate entry for the same date
        Attendance existing = attendanceRepository.findByStaff_IdAndDate(staff.getId(), attendance.getDate());
        if (existing != null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Attendance already marked for this date"));
        }

        // Validate attendance code
        String shopCode = staff.getShop().getAttendanceCode();
        String providedCode = attendance.getAttendanceCode();
        
        // Trim for safety
        shopCode = (shopCode != null) ? shopCode.trim() : null;
        providedCode = (providedCode != null) ? providedCode.trim() : null;

        System.out.println("DEBUG: Attendance Code Validation");
        System.out.println("DEBUG: Shop ID: " + staff.getShop().getId());
        System.out.println("DEBUG: Expected Code: '" + shopCode + "'");
        System.out.println("DEBUG: Provided Code: '" + providedCode + "'");

        if (shopCode != null && !shopCode.equalsIgnoreCase(providedCode)) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid attendance code"));
        } else if (shopCode == null && providedCode != null && !providedCode.isEmpty()) {
             return ResponseEntity.badRequest().body(new MessageResponse("Error: Shop has not set an attendance code, but one was provided."));
        }

        attendance.setStaff(staff);
        attendance.setShop(staff.getShop());
        attendance.setStatus("PRESENT");
        
        attendanceRepository.save(attendance);

        return ResponseEntity.ok(new MessageResponse("Attendance marked successfully"));
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<Attendance>> getStaffAttendance(@PathVariable Long staffId) {
        List<Attendance> history = attendanceRepository.findByStaff_IdOrderByDateDesc(staffId);
        return ResponseEntity.ok(history);
    }
}
