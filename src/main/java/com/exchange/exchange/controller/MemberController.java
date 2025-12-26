package com.exchange.exchange.controller;

import com.exchange.exchange.entity.Member;
import com.exchange.exchange.service.MemberService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    @Autowired
    private MemberService memberService;

    // 註冊會員
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Member member) {
        try {
            Member newMember = memberService.register(member.getAccount(), member.getPassword(), member.getName(), member.getNumber());
            return ResponseEntity.ok(newMember);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 登入
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Member member, HttpSession session) {
        Member loggedInMember = memberService.login(member.getAccount(), member.getPassword());
        if (loggedInMember != null) {
            session.setAttribute("memberId", loggedInMember.getMemberId());
            return ResponseEntity.ok(loggedInMember);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    // 登出
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Logged out successfully");
    }

    // 查看自己的資料
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(HttpSession session) {
        Integer memberId = (Integer) session.getAttribute("memberId");
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login first");
        }

        Optional<Member> member = memberService.getMemberById(memberId);
        return member.map(ResponseEntity::ok)
                     .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 修改自己的資料
    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(@RequestBody Member member, HttpSession session) {
        Integer memberId = (Integer) session.getAttribute("memberId");
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login first");
        }

        // 使用 Service 更新，包含密碼變更邏輯
        Member updatedMember = memberService.updateMember(
                memberId,
                member.getName(),
                member.getNumber(),
                member.getPassword() // 前端若傳送空值，Service 層會忽略
        );

        if (updatedMember != null) {
            return ResponseEntity.ok(updatedMember);
        }
        return ResponseEntity.notFound().build();
    }
}