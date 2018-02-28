package benchly.database;

import java.sql.SQLException;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.model.User;

public class OrmliteRealm extends AuthorizingRealm {

	private static final Logger LOG = LoggerFactory.getLogger(OrmliteRealm.class);

	public OrmliteRealm() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authToken) throws AuthenticationException {
		final String name = authToken.getPrincipal().toString();
		AuthenticationInfo result = null;

		LOG.debug("Looking for user with name: " + name);
		User user = null;
		try {
			user = UserDao.fetchByName(name);
		} catch (SQLException e) {
			LOG.error("Unexpected SQL exception during user authentication: " + e.getMessage());
			e.printStackTrace();
			user = null;
		}

		if (user != null) {
			if (user.isDeleted()) {
				LOG.warn("Not loggin in deleted user.");
			} else {
				ByteSource salt = ByteSource.Util.bytes(user.getPasswordSalt());
				result = new SimpleAuthenticationInfo(user.getId(), user.getPasswordHash(), salt, getClass().getName());
			}
		} else {
			LOG.error("Unable to fetch user for authentication.");
		}
		
		return result;
	}

}
