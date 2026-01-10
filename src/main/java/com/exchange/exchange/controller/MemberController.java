package com.exchange.exchange.controller;

// 引入實體與服務層
import com.exchange.exchange.entity.Member;
import com.exchange.exchange.service.MemberService;
// 引入 Spring Web MVC 與 Session 管理工具
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

// ====== 檔案總結 ======
// MemberController 提供會員認證與管理的 RESTful API。
// 核心功能：
// 1. 身份認證：註冊 (POST /register)、登入 (POST /login)、登出 (POST /logout)。
// 2. 個人資料管理：查詢 (GET /me)、更新 (PUT /me)。
// 狀態管理方式：使用 HttpSession (Stateful)。
@RestController
@RequestMapping("/api/members")
public class MemberController {

    // 注入 MemberService 處理業務邏輯
    @Autowired
    private MemberService memberService;

    // API：註冊新會員
    // 路徑：POST /api/members/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Member member) {
        try {
            // 呼叫 Service 進行註冊
            // 注意：這裡直接使用 Entity 接收參數，前端需傳送符合 Member 結構的 JSON
            Member newMember = memberService.register(member.getAccount(), member.getPassword(), member.getName(), member.getNumber());
            // 註冊成功，回傳新建立的會員資料 (HTTP 200)
            return ResponseEntity.ok(newMember);
        } catch (RuntimeException e) {
            // 捕捉業務異常 (如帳號重複) -> 回傳 HTTP 400 Bad Request
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API：會員登入
    // 路徑：POST /api/members/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Member member, HttpSession session) {
        // 呼叫 Service 驗證帳號密碼
        Member loggedInMember = memberService.login(member.getAccount(), member.getPassword());
        
        // 邏輯判斷：若 Service 回傳非 null 物件，表示驗證成功
        if (loggedInMember != null) {
            // 狀態管理：將 memberId 寫入 Session，標記該使用者已登入 [註1]
            session.setAttribute("memberId", loggedInMember.getMemberId());
            // 回傳會員資料
            return ResponseEntity.ok(loggedInMember);
        }
        // 驗證失敗：回傳 HTTP 401 Unauthorized
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    // API：會員登出
    // 路徑：POST /api/members/logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        // 銷毀 Session：清除伺服器端儲存的該使用者所有狀態
        session.invalidate();
        return ResponseEntity.ok("Logged out successfully");
    }

    // API：獲取我的個人資料
    // 路徑：GET /api/members/me
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(HttpSession session) {
        // 從 Session 中提取 memberId
        Integer memberId = (Integer) session.getAttribute("memberId");
        
        // 權限驗證：若 Session 中無 ID，表示未登入或 Session 已過期
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login first");
        }

        // 查詢會員資料
        Optional<Member> member = memberService.getMemberById(memberId);
        
        // 若找不到會員 (可能被管理員刪除)，回傳 404；否則回傳資料
        return member.map(ResponseEntity::ok)
                     .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // API：更新我的個人資料
    // 路徑：PUT /api/members/me
    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(@RequestBody Member member, HttpSession session) {
        // 從 Session 獲取當前登入者 ID
        Integer memberId = (Integer) session.getAttribute("memberId");
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login first");
        }

        // 呼叫 Service 執行更新
        // 前端只需傳送欲修改的欄位，未傳送的欄位 (null) 將被忽略
        Member updatedMember = memberService.updateMember(
                memberId,
                member.getName(),
                member.getNumber(),
                member.getPassword() 
        );

        if (updatedMember != null) {
            return ResponseEntity.ok(updatedMember);
        }
        return ResponseEntity.notFound().build();
    }
}

// ====== 備註區 ======
/*
[註1] Session 架構限制 (Session Management):
      目前使用 Servlet `HttpSession` 進行狀態管理。
      這在單體應用 (Monolith) 中運作良好，但在分散式系統或前後端分離架構中，Session 擴展性較差。
      建議改用 Token-Based 驗證 (如 JWT) 搭配 Spring Security，實現無狀態 (Stateless) 認證。

[註2] 資料暴露風險 (Data Exposure):
      `register` 與 `login` 接口直接回傳 `Member` Entity。
      這會導致後端資料結構 (如 `password` 欄位) 暴露給前端 (即使有 @JsonIgnore 設定，DTO 模式仍較安全)。
      建議建立專用的 `MemberResponseDTO`，僅包含 `id`, `name`, `account` 等非敏感欄位。
*/