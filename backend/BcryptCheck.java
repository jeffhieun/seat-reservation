import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class BcryptCheck {
  public static void main(String[] args) {
    BCryptPasswordEncoder e = new BCryptPasswordEncoder(10);
    System.out.println(e.matches("password123","$2a$10$dXJ3SUicVNrDY2Z8VHMh.eC2xVRMLNWFvD0jGDbNMqfHVNmXy1h0a"));
  }
}
