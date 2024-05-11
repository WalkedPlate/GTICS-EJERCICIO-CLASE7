package com.example.gticsejercicioclase7.config;

import com.example.gticsejercicioclase7.repository.UsersRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;

import javax.sql.DataSource;

@Configuration
public class WebSecurityConfig {


    final DataSource dataSource;
    final UsersRepository usersRepository;
    public WebSecurityConfig(DataSource dataSource, UsersRepository usersRepository) {
        this.dataSource = dataSource;
        this.usersRepository = usersRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsManager users(DataSource dataSource){
        JdbcUserDetailsManager users = new JdbcUserDetailsManager(dataSource);
        String sql1 = "SELECT email, password FROM users WHERE email = ?";
        String sql2 = "SELECT u.email, r.name FROM users u "
                + "INNER JOIN roles r ON (u.idrol = r.id) "
                + "WHERE u.email = ?";

        users.setUsersByUsernameQuery(sql1);
        users.setAuthoritiesByUsernameQuery(sql2);
        return users;
    }



    /*
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{

        http.formLogin()
           .loginPage("/loginForm")
           .loginProcessingUrl("/processLogin");

        http.authorizeHttpRequests()
           .requestMatchers("/employee", "/employee/**").hasAnyAuthority("admin", "logistica")
           .requestMatchers("/shipper", "/shipper/**").hasAuthority("admin")
           .anyRequest().permitAll();

        return http.build();
    }*/

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.formLogin(formLogin ->
                formLogin
                        .loginPage("/openLoginWindow")
                        .loginProcessingUrl("/submitLoginForm")
                        .successHandler((request, response, authentication) -> {

                            DefaultSavedRequest defaultSavedRequest =
                                    (DefaultSavedRequest) request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");

                            HttpSession session = request.getSession();
                            session.setAttribute("usuario", usersRepository.findByEmail(authentication.getName()));


                            //si vengo por url -> defaultSR existe
                            if (defaultSavedRequest != null) {
                                String targetURl = defaultSavedRequest.getRequestURL();
                                new DefaultRedirectStrategy().sendRedirect(request, response, targetURl);
                            } else { //estoy viniendo del botón de login
                                String rol = "";
                                for (GrantedAuthority role : authentication.getAuthorities()) {
                                    rol = role.getAuthority();
                                    break;
                                }

                                if (rol.equals("admin")) {
                                    response.sendRedirect("/shipper");
                                } else {
                                    response.sendRedirect("/employee");
                                }
                            }
                        }));


        http.authorizeHttpRequests(authz -> authz
                                .requestMatchers("/personaje/list").hasAnyRole("USER", "EDITOR", "ADMIN")
                                .requestMatchers("/personaje/new").hasAnyRole("EDITOR", "ADMIN")
                                .requestMatchers("/personaje/edit").hasAnyRole("EDITOR", "ADMIN")
                                .requestMatchers("/personaje/delete").hasAnyRole("ADMIN")
                                .anyRequest().permitAll());
                //.httpBasic(Customizer.withDefaults())
                //.sessionManagement(httpSecuritySessionManagementConfigurer -> httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

}
