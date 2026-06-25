package com.zjhl.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjhl.project.entity.SignPayment;
import com.zjhl.project.mapper.SignPaymentMapper;
import com.zjhl.project.service.SignPaymentService;
import org.springframework.stereotype.Service;

@Service
public class SignPaymentServiceImpl extends ServiceImpl<SignPaymentMapper, SignPayment> implements SignPaymentService {
}
