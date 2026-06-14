package org.springframework.samples.petclinic.repository.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.samples.petclinic.model.Role;
import org.springframework.samples.petclinic.model.User;

class JdbcUserRepositoryImplTest {

	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private SimpleJdbcInsert insertUser;
	private JdbcUserRepositoryImpl repository;

	@BeforeEach
	void setUp() {
		namedParameterJdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
		insertUser = Mockito.mock(SimpleJdbcInsert.class);
		repository = new JdbcUserRepositoryImpl(Mockito.mock(DataSource.class));
		setField(repository, "namedParameterJdbcTemplate", namedParameterJdbcTemplate);
		setField(repository, "insertUser", insertUser);
	}

	private void setField(Object target, String fieldName, Object value) {
		try {
			var field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private User invokeGetByUsername(JdbcUserRepositoryImpl repo, String username) {
		try {
			Method method = JdbcUserRepositoryImpl.class.getDeclaredMethod("getByUsername", String.class);
			method.setAccessible(true);
			return (User) method.invoke(repo, username);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void save_existingUser_updatesUserAndRoles() {
		// Arrange
		User existingUser = new User();
		existingUser.setUsername("user1");
		existingUser.setPassword("oldpass");
		existingUser.setEnabled(true);
		Set<Role> roles = new HashSet<>();
		Role role1 = new Role();
		role1.setName("ROLE_USER");
		roles.add(role1);
		existingUser.setRoles(roles);

		when(namedParameterJdbcTemplate.queryForObject(Mockito.anyString(), Mockito.anyMap(), Mockito.any(BeanPropertyRowMapper.class)))
			.thenReturn(existingUser);
		when(namedParameterJdbcTemplate.update(eq("UPDATE users SET password=:password, enabled=:enabled WHERE username=:username"), any(BeanPropertySqlParameterSource.class)))
			.thenReturn(1);
		when(namedParameterJdbcTemplate.update(eq("DELETE FROM roles WHERE username=:username"), anyMap()))
			.thenReturn(1);
		when(namedParameterJdbcTemplate.update(eq("INSERT INTO roles(username, role) VALUES (:username, :role)"), anyMap()))
			.thenReturn(1);

		User userToSave = new User();
		userToSave.setUsername("user1");
		userToSave.setPassword("newpass");
		userToSave.setEnabled(false);
		Set<Role> newRoles = new HashSet<>();
		Role newRole = new Role();
		newRole.setName("ROLE_ADMIN");
		newRoles.add(newRole);
		userToSave.setRoles(newRoles);

		// Act
		assertThatCode(() -> repository.save(userToSave)).doesNotThrowAnyException();

		// Assert
		verify(namedParameterJdbcTemplate).queryForObject(Mockito.anyString(), Mockito.anyMap(), Mockito.any(BeanPropertyRowMapper.class));
		verify(namedParameterJdbcTemplate).update(eq("UPDATE users SET password=:password, enabled=:enabled WHERE username=:username"), any(BeanPropertySqlParameterSource.class));
		verify(namedParameterJdbcTemplate).update(eq("DELETE FROM roles WHERE username=:username"), anyMap());
		verify(namedParameterJdbcTemplate, times(1)).update(eq("INSERT INTO roles(username, role) VALUES (:username, :role)"), anyMap());
		verify(insertUser, never()).execute(any(BeanPropertySqlParameterSource.class));
	}

	@Test
	void save_newUser_insertsUserAndUpdatesRoles() {
		// Arrange
		when(namedParameterJdbcTemplate.queryForObject(Mockito.anyString(), Mockito.anyMap(), Mockito.any(BeanPropertyRowMapper.class)))
			.thenThrow(new EmptyResultDataAccessException(1));
		when(insertUser.execute(Mockito.any(BeanPropertySqlParameterSource.class))).thenReturn(1);
		when(namedParameterJdbcTemplate.update(eq("DELETE FROM roles WHERE username=:username"), anyMap()))
			.thenReturn(1);
		when(namedParameterJdbcTemplate.update(eq("INSERT INTO roles(username, role) VALUES (:username, :role)"), anyMap()))
			.thenReturn(1);

		User newUser = new User();
		newUser.setUsername("newuser");
		newUser.setPassword("pass");
		newUser.setEnabled(true);
		Set<Role> roles = new HashSet<>();
		Role role1 = new Role();
		role1.setName("ROLE_USER");
		roles.add(role1);
		newUser.setRoles(roles);

		// Act
		assertThatCode(() -> repository.save(newUser)).doesNotThrowAnyException();

		// Assert
		verify(namedParameterJdbcTemplate).queryForObject(Mockito.anyString(), Mockito.anyMap(), Mockito.any(BeanPropertyRowMapper.class));
		verify(insertUser).execute(any(BeanPropertySqlParameterSource.class));
		verify(namedParameterJdbcTemplate).update(eq("DELETE FROM roles WHERE username=:username"), anyMap());
		verify(namedParameterJdbcTemplate, times(1)).update(eq("INSERT INTO roles(username, role) VALUES (:username, :role)"), anyMap());
		verify(namedParameterJdbcTemplate, never()).update(eq("UPDATE users SET password=:password, enabled=:enabled WHERE username=:username"), any(BeanPropertySqlParameterSource.class));
	}

	@Test
	void save_userWithNullRoleName_skipsRoleInsertion() {
		// Arrange
		when(namedParameterJdbcTemplate.queryForObject(Mockito.anyString(), Mockito.anyMap(), Mockito.any(BeanPropertyRowMapper.class)))
			.thenThrow(new EmptyResultDataAccessException(1));
		when(insertUser.execute(Mockito.any(BeanPropertySqlParameterSource.class))).thenReturn(1);
		when(namedParameterJdbcTemplate.update(eq("DELETE FROM roles WHERE username=:username"), anyMap()))
			.thenReturn(1);
		when(namedParameterJdbcTemplate.update(eq("INSERT INTO roles(username, role) VALUES (:username, :role)"), anyMap()))
			.thenReturn(1);

		User user = new User();
		user.setUsername("userWithNullRole");
		user.setPassword("pass");
		user.setEnabled(true);
		Set<Role> roles = new HashSet<>();
		Role roleWithName = new Role();
		roleWithName.setName("ROLE_USER");
		Role roleWithNullName = new Role();
		roleWithNullName.setName(null);
		roles.add(roleWithName);
		roles.add(roleWithNullName);
		user.setRoles(roles);

		// Act
		assertThatCode(() -> repository.save(user)).doesNotThrowAnyException();

		// Assert
		verify(namedParameterJdbcTemplate).queryForObject(Mockito.anyString(), Mockito.anyMap(), Mockito.any(BeanPropertyRowMapper.class));
		verify(insertUser).execute(any(BeanPropertySqlParameterSource.class));
		verify(namedParameterJdbcTemplate).update(eq("DELETE FROM roles WHERE username=:username"), anyMap());
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(namedParameterJdbcTemplate, times(1)).update(eq("INSERT INTO roles(username, role) VALUES (:username, :role)"), captor.capture());
		Map<String, Object> params = captor.getValue();
		assertThat(params.get("role")).isEqualTo("ROLE_USER");
	}

	@Test
	void getByUsername_existingUsername_returnsUser() {
		// Arrange
		User user = new User();
		user.setUsername("existingUser");
		user.setPassword("pass");
		user.setEnabled(true);

		when(namedParameterJdbcTemplate.queryForObject(Mockito.anyString(), Mockito.anyMap(), Mockito.any(BeanPropertyRowMapper.class)))
			.thenReturn(user);

		// Act
		User result = invokeGetByUsername(repository, "existingUser");

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getUsername()).isEqualTo("existingUser");
		assertThat(result.getPassword()).isEqualTo("pass");
		assertThat(result.isEnabled()).isTrue();
	}

	@Test
	void getByUsername_nonExistingUsername_throwsEmptyResultDataAccessException() {
		// Arrange
		when(namedParameterJdbcTemplate.queryForObject(Mockito.anyString(), Mockito.anyMap(), Mockito.any(BeanPropertyRowMapper.class)))
			.thenThrow(new EmptyResultDataAccessException(1));

		// Act & Assert
		assertThatThrownBy(() -> invokeGetByUsername(repository, "nonExistingUser"))
			.isInstanceOf(EmptyResultDataAccessException.class);
	}
}
