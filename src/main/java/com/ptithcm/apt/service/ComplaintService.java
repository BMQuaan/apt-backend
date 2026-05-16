package com.ptithcm.apt.service;

import com.ptithcm.apt.dto.request.CreateComplaintRequest;
import com.ptithcm.apt.dto.request.UpdateComplaintStatusRequest;
import com.ptithcm.apt.dto.response.ComplaintResponse;

import java.util.List;

public interface ComplaintService {
    ComplaintResponse createComplaint(CreateComplaintRequest request);

    List<ComplaintResponse> getMyComplaints();

    List<ComplaintResponse> getAllComplaints();

    ComplaintResponse updateStatus(Long id, UpdateComplaintStatusRequest request);
}
