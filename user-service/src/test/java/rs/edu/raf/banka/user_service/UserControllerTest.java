package rs.edu.raf.banka.user_service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import rs.edu.raf.banka.user_service.controller.UserController;
import rs.edu.raf.banka.user_service.controller.response_forms.ChangePasswordForm;
import rs.edu.raf.banka.user_service.controller.response_forms.CreateUserForm;
import rs.edu.raf.banka.user_service.controller.response_forms.ResetPasswordForm;
import rs.edu.raf.banka.user_service.model.Role;
import rs.edu.raf.banka.user_service.model.User;
import rs.edu.raf.banka.user_service.service.implementation.UserServiceImplementation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(UserController.class)
public class UserControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserServiceImplementation userServiceImplementation;

    String validJWToken = initValidJWT();
    String invalidJWToken = initInvalidJWT();
    CreateUserForm userMockForm = initUserMockForm();
    ResetPasswordForm resetPasswordForm = initResetPasswordForm();
    ChangePasswordForm changePasswordForm = initChangePasswordForm();
    String dummyName = "Mock";

    @Test
    void testGetUsersAPI() throws Exception {
        List<User> users = new ArrayList<>();
        User user = new User(dummyName, "Test");
        users.add(user);

        String jsonResult = "[{\"id\":0,\"username\":\"Mock\",\"ime\":null,\"prezime\":null,\"email\":null,\"jmbg\":null,\"br_telefon\":null,\"password\":\"Test\",\"otpSeecret\":null,\"aktivan\":false,\"role\":null,\"requiresOtp\":false}]";

        when(userServiceImplementation.getUsers()).thenReturn(users);

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + validJWToken))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResult));
    }

    @Test
    void testGetUsersInvalidToken() throws Exception{
        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidJWToken))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testCreateUserAPI() throws Exception {
        String jsonResult = "{\"id\":0,\"username\":\"Mock\",\"ime\":null,\"prezime\":null,\"email\":null,\"jmbg\":null,\"br_telefon\":null,\"password\":\"Test\",\"otpSeecret\":null,\"aktivan\":false,\"role\":null,\"requiresOtp\":false}";

        when(userServiceImplementation.createUser(userMockForm)).thenReturn(new User(dummyName, "Test"));


        mockMvc.perform(post("/api/user/create").header(HttpHeaders.AUTHORIZATION, "Bearer " + validJWToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userMockForm)))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResult));
    }

    @Test
    void testEditPermissionsAPI() throws Exception{
        User user = new User(dummyName, "Test");
        user.setId(2L);
        user.setRole(new Role(null,"ADMIN_ROLE", List.of(new String[]{"ADMIN_MOCK"})));

        given(userServiceImplementation.getUserById(2L)).willReturn(Optional.of(user));

        when(userServiceImplementation.hasEditPermissions(user, "Bearer " + validJWToken)).thenReturn(true);

        doNothing()
            .when(userServiceImplementation)
            .editUser(user, userMockForm);

        mockMvc.perform(post("/api/user/edit/{id}", 2L).header(HttpHeaders.AUTHORIZATION, "Bearer " + validJWToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userMockForm)))
                .andExpect(status().isOk())
                .andExpect(content().string(dummyName + " edited"));
    }

    @Test
    void testInvalidEditPermissions() throws Exception{
        mockMvc.perform(post("/api/user/edit/{id}", 2L).header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidJWToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(userMockForm)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testDeleteAPI() throws Exception{
        User user = new User(dummyName, "Test");
        user.setId(2L);
        user.setRole(new Role(null,"ADMIN_ROLE", List.of(new String[]{"ADMIN_MOCK"})));

        given(userServiceImplementation.getUserById(2L)).willReturn(Optional.of(user));

        when(userServiceImplementation.deleteUser(user)).thenReturn(true);

        mockMvc.perform(delete("/api/user/delete/{id}", 2L).header(HttpHeaders.AUTHORIZATION, "Bearer " + validJWToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(userMockForm)))
                .andExpect(status().isOk())
                .andExpect(content().string(dummyName + " disabled"));
    }

    @Test
    void testInvalidDeleteAPI() throws Exception{
        given(userServiceImplementation.getUserById(2L)).willReturn(null);

        when(userServiceImplementation.deleteUser(any())).thenReturn(true);

        mockMvc.perform(delete("/api/user/delete/{id}", 2L).header(HttpHeaders.AUTHORIZATION, "Bearer " + validJWToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }




    @Test
    void testGetUser() throws Exception{
        User user = new User(dummyName, "Test");
        user.setId(2L);
        user.setRole(new Role(null,"ADMIN_ROLE", List.of(new String[]{"ADMIN_MOCK"})));

        when(userServiceImplementation.getUserByToken(anyString())).thenReturn(user);

        mockMvc.perform(get("/api/user", 2L).header(HttpHeaders.AUTHORIZATION, "Bearer " + validJWToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testInvalidGetUser() throws Exception{
        when(userServiceImplementation.getUserByToken(anyString())).thenReturn(null);

        mockMvc.perform(get("/api/user", 2L).header(HttpHeaders.AUTHORIZATION, "Bearer " + validJWToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testInvalidAuthGetUser() throws Exception{
        User user = new User(dummyName, "Test");
        user.setId(2L);
        user.setRole(new Role(null,"ADMIN_ROLE", List.of(new String[]{"ADMIN_MOCK"})));

        when(userServiceImplementation.getUserByToken(anyString())).thenReturn(user);

        mockMvc.perform(get("/api/user", 2L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }



    @Test
    void testEditUserFromToken() throws Exception{
        User user = new User(dummyName, "Test");
        user.setId(2L);
        user.setRole(new Role(null,"ADMIN_ROLE", List.of(new String[]{"ADMIN_MOCK"})));

        when(userServiceImplementation.hasEditPermissions(any(),anyString())).thenReturn(true);
        when(userServiceImplementation.getUserByToken(anyString())).thenReturn(user);

        mockMvc.perform(patch("/api/user", 2L).header(HttpHeaders.AUTHORIZATION, "Bearer " + validJWToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(userMockForm)))
                .andExpect(status().isOk());
    }

    @Test
    void testEditUserFromTokenInvalid() throws Exception{
        User user = new User(dummyName, "Test");
        user.setId(2L);
        user.setRole(new Role(null,"ADMIN_ROLE", List.of(new String[]{"ADMIN_MOCK"})));

        when(userServiceImplementation.hasEditPermissions(any(),anyString())).thenReturn(true);
        when(userServiceImplementation.getUserByToken(anyString())).thenReturn(user);

        mockMvc.perform(patch("/api/user", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(userMockForm)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEditUserFromInvalidToken() throws Exception{
        mockMvc.perform(patch("/api/user", 2L).header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidJWToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(userMockForm)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testResetPassword() throws Exception{
        when(userServiceImplementation.resetPassword("mock@test")).thenReturn(true);

        mockMvc.perform(post("/api/user/reset-password", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(resetPasswordForm)))
                .andExpect(status().isOk())
                .andExpect(content().string("Mail send to: " + resetPasswordForm.getEmail()));
    }

    @Test
    void testInvalidResetPassword() throws Exception{
        mockMvc.perform(post("/api/user/reset-password", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(resetPasswordForm)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Mail failed to send"));
    }

    @Test
    void testChangePassword() throws Exception{

        when(userServiceImplementation.setNewPassword(anyString(),anyString())).thenReturn(true);

        mockMvc.perform(post("/api/user/change-password", 2L).header(HttpHeaders.AUTHORIZATION, "Bearer " + validJWToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(changePasswordForm)))
                .andExpect(status().isOk())
                .andExpect(content().string("New password!"));

    }

    @Test
    void testInvalidChangePassword() throws Exception{
        mockMvc.perform(post("/api/user/change-password", 2L).header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidJWToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(changePasswordForm)))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string("Invalid token!"));
    }
    

    @Test
    void testInvalidNewPassword() throws Exception{
        User user = new User(dummyName, "Test");
        user.setId(2L);
        user.setRole(new Role(null,"ADMIN_ROLE", List.of(new String[]{"ADMIN_MOCK"})));

        when(userServiceImplementation.getUserById(2L)).thenReturn(null);
        when(userServiceImplementation.changePassword(any(),any())).thenReturn(true);

        mockMvc.perform(post("/api/user/new-password/", 2L).header(HttpHeaders.AUTHORIZATION, "Bearer " + validJWToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(changePasswordForm)))
                .andExpect(status().is4xxClientError());
    }


    @Test
    void testInvalidDelete() throws Exception{
        mockMvc.perform(delete("/api/user/delete/{id}", 2L).header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidJWToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(userMockForm)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testInvalidAdminDelete() throws Exception{
        User user = new User("admin", "Test");
        user.setId(1L);
        user.setRole(new Role(null,"GL_ADMIN_ROLE", List.of(new String[]{"GL_ADMIN_MOCK"})));

        given(userServiceImplementation.getUserById(1L)).willReturn(Optional.of(user));
        when(userServiceImplementation.deleteUser(user)).thenReturn(false);


        mockMvc.perform(delete("/api/user/delete/{id}", 1L).header(HttpHeaders.AUTHORIZATION, "Bearer " + validJWToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(userMockForm)))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string("Can't delete admin"));
    }


    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    String initValidJWT(){
        return JWT.create()
                .withSubject(dummyName)
                .withIssuer("mock")
                .withClaim("permissions", Arrays.asList(new String[]{"CREATE_USER", "LIST_USERS", "EDIT_USER", "MY_EDIT", "DELETE_USER"}))
                .sign(Algorithm.HMAC256("secret".getBytes()));
    }

    String initInvalidJWT(){
        return JWT.create()
                .withSubject(dummyName)
                .withIssuer("mock")
                .withClaim("permissions", Arrays.asList(new String[]{"X_LIST_USERS", "DUMMY_FAKE_PERMISSION"}))
                .sign(Algorithm.HMAC256("secret".getBytes()));
    }

    private CreateUserForm initUserMockForm() {
        CreateUserForm userMockForm = new CreateUserForm();
        userMockForm.setIme(dummyName);
        userMockForm.setPrezime("Test");
        userMockForm.setEmail("mock@test");
        userMockForm.setJmbg("123");
        userMockForm.setBr_telefon("123");
        userMockForm.setPozicija("ROLE_ADMIN");

        return userMockForm;
    }

    private ResetPasswordForm initResetPasswordForm() {
        ResetPasswordForm resetPassMockForm = new ResetPasswordForm();
        resetPassMockForm.setEmail("mock@test");
        return resetPassMockForm;
    }

    private ChangePasswordForm initChangePasswordForm() {
        ChangePasswordForm changePasswordForm = new ChangePasswordForm();
        changePasswordForm.setNewPassword("mockPass");
        return  changePasswordForm;
    }

}
