package com.attirehub.returns.service;

import com.attirehub.returns.dto.CreateReturnRequestDto;
import com.attirehub.returns.dto.ReturnRequestResponse;
import com.attirehub.shared.dto.PagedResponse;

public interface ReturnRequestService {

    void submitPublic(CreateReturnRequestDto request);

    PagedResponse<ReturnRequestResponse> listForAdmin(int page, int size);
}
