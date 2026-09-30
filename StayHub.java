import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * StayHub - Single File Java Full-Stack House Rental Website
 *
 * Requirements:
 *   Java 17+ (Java 21/25 also works)
 *
 * Run:
 *   javac StayHub.java
 *   java StayHub
 *
 * Open:
 *   http://localhost:8080
 *
 * This is a self-contained demo:
 * - Java HTTP server/backend
 * - HTML + CSS + JavaScript frontend
 * - Registration/Login with Owner/Renter roles
 * - Owner property management
 * - House photographs
 * - Property details
 * - Rental booking requests
 * - Renter booking history
 *
 * Data is stored in memory, so it resets when the application restarts.
 */
public class StayHub {

    private static final int PORT = 8080;
    private static final List<User> users = Collections.synchronizedList(new ArrayList<>());
    private static final List<Property> properties = Collections.synchronizedList(new ArrayList<>());
    private static final List<Booking> bookings = Collections.synchronizedList(new ArrayList<>());
    private static final AtomicInteger propertySeq = new AtomicInteger(6);
    private static final AtomicInteger bookingSeq = new AtomicInteger(1);

    // House photographs. They are loaded from Unsplash so the Java project remains one file.
    private static final String[] HOUSE_IMAGES = {
        "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=1000&q=85",
        "https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=1000&q=85",
        "https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1000&q=85",
        "https://images.unsplash.com/photo-1600607687920-4e2a09cf159d?auto=format&fit=crop&w=1000&q=85",
        "https://images.unsplash.com/photo-1600047509807-ba8f99d2cdde?auto=format&fit=crop&w=1000&q=85"
    };

    static class User {
        String id, name, email, password, role;
        User(String id, String name, String email, String password, String role) {
            this.id = id; this.name = name; this.email = email; this.password = password; this.role = role;
        }
    }

    static class Property {
        int id;
        String type, adType, address, contact, details, image;
        double amount;
        String ownerId, ownerName;
        Property(int id, String type, String adType, String address, double amount,
                 String contact, String details, String image, String ownerId, String ownerName) {
            this.id=id; this.type=type; this.adType=adType; this.address=address;
            this.amount=amount; this.contact=contact; this.details=details;
            this.image=image; this.ownerId=ownerId; this.ownerName=ownerName;
        }
    }

    static class Booking {
        int id, propertyId;
        String renterId, renterName, phone, moveInDate, message, status;
        Booking(int id, int propertyId, String renterId, String renterName,
                String phone, String moveInDate, String message, String status) {
            this.id=id; this.propertyId=propertyId; this.renterId=renterId;
            this.renterName=renterName; this.phone=phone; this.moveInDate=moveInDate;
            this.message=message; this.status=status;
        }
    }

    public static void main(String[] args) throws Exception {
        seedData();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", StayHub::handle);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("==============================================");
        System.out.println(" StayHub House Rental - Java Full Stack");
        System.out.println(" Running at http://localhost:" + PORT);
        System.out.println("==============================================");
    }

    private static void seedData() {
        if (!users.isEmpty()) return;

        User owner = new User("u1", "Test1", "owner@stayhub.com", "1234", "owner");
        User renter = new User("u2", "User2", "renter@stayhub.com", "1234", "renter");
        users.add(owner);
        users.add(renter);

        properties.add(new Property(
            1, "Residential", "Rent",
            "Flat 503, Silver Oak Towers, Wakad - Hinjewadi Link Road, Pune - 411057",
            25000, "9865327410",
            "Well-ventilated 2BHK flat with wooden flooring in the master bedroom, modular kitchen setup, piped gas connection, RO water purifier, 24/7 security with smart intercom, and 1 covered car parking space.",
            HOUSE_IMAGES[0], owner.id, owner.name
        ));
        properties.add(new Property(
            2, "Residential", "Rent",
            "Green Valley Apartments, Baner Road, Pune - 411045",
            22000, "9876543210",
            "Spacious 2BHK apartment with balcony, modular kitchen, lift, security and covered parking.",
            HOUSE_IMAGES[1], owner.id, owner.name
        ));
        properties.add(new Property(
            3, "Residential", "Rent",
            "Lake View Residency, Kharadi, Pune - 411014",
            28000, "9823456710",
            "Bright 2BHK home near IT parks with clubhouse, gym and children's play area.",
            HOUSE_IMAGES[2], owner.id, owner.name
        ));
        properties.add(new Property(
            4, "Residential", "Rent",
            "Skyline Heights, Viman Nagar, Pune - 411014",
            30000, "9812345670",
            "Premium 2BHK flat with modern interiors, large windows, balcony and excellent connectivity.",
            HOUSE_IMAGES[3], owner.id, owner.name
        ));
        properties.add(new Property(
            5, "Residential", "Rent",
            "Riverstone Homes, Hinjewadi Phase 1, Pune - 411057",
            24000, "9898989898",
            "Comfortable family apartment with parking, power backup, security and nearby shopping.",
            HOUSE_IMAGES[4], owner.id, owner.name
        ));
    }

    private static void handle(HttpExchange ex) throws IOException {
        addCors(ex);
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            send(ex, 204, "");
            return;
        }

        try {
            if (path.equals("/") || path.equals("/index.html")) {
                send(ex, 200, PAGE);
                return;
            }

            if (path.equals("/api/register") && method.equalsIgnoreCase("POST")) {
                Map<String,String> f = formData(ex);
                String name = f.getOrDefault("name","");
                String email = f.getOrDefault("email","").toLowerCase();
                String password = f.getOrDefault("password","");
                String role = f.getOrDefault("role","").toLowerCase();

                if (name.isBlank() || email.isBlank() || password.isBlank() ||
                    (!role.equals("owner") && !role.equals("renter"))) {
                    json(ex, 400, "{\"ok\":false,\"message\":\"Please fill all registration fields.\"}");
                    return;
                }

                synchronized (users) {
                    for (User u : users) {
                        if (u.email.equalsIgnoreCase(email)) {
                            json(ex, 409, "{\"ok\":false,\"message\":\"Email is already registered.\"}");
                            return;
                        }
                    }
                    String id = "u" + (users.size()+1);
                    users.add(new User(id, name, email, password, role));
                    json(ex, 200, "{\"ok\":true,\"message\":\"Registration successful. Please sign in.\"}");
                }
                return;
            }

            if (path.equals("/api/login") && method.equalsIgnoreCase("POST")) {
                Map<String,String> f = formData(ex);
                String email = f.getOrDefault("email","").toLowerCase();
                String password = f.getOrDefault("password","");

                synchronized (users) {
                    for (User u : users) {
                        if (u.email.equalsIgnoreCase(email) && u.password.equals(password)) {
                            json(ex, 200, "{\"ok\":true,\"user\":"+userJson(u)+"}");
                            return;
                        }
                    }
                }
                json(ex, 401, "{\"ok\":false,\"message\":\"Invalid email or password.\"}");
                return;
            }

            if (path.equals("/api/properties") && method.equalsIgnoreCase("GET")) {
                StringBuilder a = new StringBuilder("[");
                synchronized (properties) {
                    for (int i=0;i<properties.size();i++) {
                        if (i>0) a.append(",");
                        a.append(propertyJson(properties.get(i)));
                    }
                }
                a.append("]");
                json(ex, 200, a.toString());
                return;
            }

            if (path.equals("/api/properties") && method.equalsIgnoreCase("POST")) {
                Map<String,String> f = formData(ex);
                String ownerId = f.getOrDefault("ownerId","");
                User owner = findUser(ownerId);
                if (owner == null || !owner.role.equals("owner")) {
                    json(ex, 403, "{\"ok\":false,\"message\":\"Only an owner can add a property.\"}");
                    return;
                }

                String image = f.getOrDefault("image","");
                if (image.isBlank()) image = HOUSE_IMAGES[(propertySeq.get()-1) % HOUSE_IMAGES.length];

                Property p = new Property(
                    propertySeq.getAndIncrement(),
                    f.getOrDefault("type","Residential"),
                    f.getOrDefault("adType","Rent"),
                    f.getOrDefault("address",""),
                    parseDouble(f.getOrDefault("amount","0")),
                    f.getOrDefault("contact",""),
                    f.getOrDefault("details",""),
                    image, owner.id, owner.name
                );
                properties.add(p);
                json(ex, 200, "{\"ok\":true,\"message\":\"Property added successfully.\",\"property\":"+propertyJson(p)+"}");
                return;
            }

            if (path.equals("/api/bookings") && method.equalsIgnoreCase("GET")) {
                String renterId = query(ex, "renterId");
                String ownerId = query(ex, "ownerId");
                StringBuilder a = new StringBuilder("[");
                boolean first = true;

                synchronized (bookings) {
                    for (Booking b : bookings) {
                        Property p = findProperty(b.propertyId);
                        boolean match = (renterId != null && b.renterId.equals(renterId)) ||
                                        (ownerId != null && p != null && p.ownerId.equals(ownerId));
                        if (match) {
                            if (!first) a.append(",");
                            first=false;
                            a.append(bookingJson(b, p));
                        }
                    }
                }
                a.append("]");
                json(ex, 200, a.toString());
                return;
            }

            if (path.equals("/api/bookings") && method.equalsIgnoreCase("POST")) {
                Map<String,String> f = formData(ex);
                int propertyId = (int)parseDouble(f.getOrDefault("propertyId","0"));
                Property p = findProperty(propertyId);
                User renter = findUser(f.getOrDefault("renterId",""));

                if (p == null || renter == null || !renter.role.equals("renter")) {
                    json(ex, 400, "{\"ok\":false,\"message\":\"Invalid property or renter.\"}");
                    return;
                }

                Booking b = new Booking(
                    bookingSeq.getAndIncrement(), propertyId, renter.id, renter.name,
                    f.getOrDefault("phone",""), f.getOrDefault("moveInDate",""),
                    f.getOrDefault("message",""), "pending"
                );
                bookings.add(b);
                json(ex, 200, "{\"ok\":true,\"message\":\"Booking request sent successfully to the owner!\"}");
                return;
            }

            if (path.equals("/api/bookings/status") && method.equalsIgnoreCase("POST")) {
                Map<String,String> f = formData(ex);
                int id = (int)parseDouble(f.getOrDefault("bookingId","0"));
                String status = f.getOrDefault("status","pending");
                Booking b = null;
                synchronized (bookings) {
                    for (Booking x : bookings) if (x.id == id) { b=x; break; }
                }
                if (b == null) {
                    json(ex, 404, "{\"ok\":false,\"message\":\"Booking not found.\"}");
                    return;
                }
                b.status = status;
                json(ex, 200, "{\"ok\":true,\"message\":\"Booking status updated.\"}");
                return;
            }

            send(ex, 404, "Not found");
        } catch (Exception e) {
            e.printStackTrace();
            json(ex, 500, "{\"ok\":false,\"message\":\"Server error: "+escape(e.getMessage())+"\"}");
        }
    }

    private static Map<String,String> formData(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String,String> m = new HashMap<>();
        for (String part : body.split("&")) {
            if (part.isEmpty()) continue;
            String[] kv = part.split("=",2);
            String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            m.put(k,v);
        }
        return m;
    }

    private static String query(HttpExchange ex, String key) {
        String q = ex.getRequestURI().getRawQuery();
        if (q == null) return null;
        for (String p : q.split("&")) {
            String[] kv=p.split("=",2);
            if (kv[0].equals(key)) return kv.length>1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
        }
        return null;
    }

    private static User findUser(String id) {
        synchronized (users) {
            for (User u: users) if (u.id.equals(id)) return u;
        }
        return null;
    }

    private static Property findProperty(int id) {
        synchronized (properties) {
            for (Property p: properties) if (p.id==id) return p;
        }
        return null;
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch(Exception e) { return 0; }
    }

    private static String userJson(User u) {
        return "{\"id\":\""+escape(u.id)+"\",\"name\":\""+escape(u.name)+
               "\",\"email\":\""+escape(u.email)+"\",\"role\":\""+escape(u.role)+"\"}";
    }

    private static String propertyJson(Property p) {
        return "{\"id\":"+p.id+",\"type\":\""+escape(p.type)+"\",\"adType\":\""+escape(p.adType)+
               "\",\"address\":\""+escape(p.address)+"\",\"amount\":"+p.amount+
               ",\"contact\":\""+escape(p.contact)+"\",\"details\":\""+escape(p.details)+
               "\",\"image\":\""+escape(p.image)+"\",\"ownerId\":\""+escape(p.ownerId)+
               "\",\"ownerName\":\""+escape(p.ownerName)+"\"}";
    }

    private static String bookingJson(Booking b, Property p) {
        return "{\"id\":"+b.id+",\"propertyId\":"+b.propertyId+
               ",\"propertyAddress\":\""+escape(p==null?"":p.address)+
               "\",\"propertyImage\":\""+escape(p==null?"":p.image)+
               "\",\"renterName\":\""+escape(b.renterName)+"\",\"phone\":\""+escape(b.phone)+
               "\",\"moveInDate\":\""+escape(b.moveInDate)+"\",\"message\":\""+escape(b.message)+
               "\",\"status\":\""+escape(b.status)+"\"}";
    }

    private static String escape(String s) {
        if (s==null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\r"," ").replace("\n"," ");
    }

    private static void addCors(HttpExchange ex) {
        Headers h=ex.getResponseHeaders();
        h.set("Access-Control-Allow-Origin","*");
        h.set("Access-Control-Allow-Methods","GET,POST,OPTIONS");
        h.set("Access-Control-Allow-Headers","Content-Type");
    }

    private static void json(HttpExchange ex, int status, String body) throws IOException {
        ex.getResponseHeaders().set("Content-Type","application/json; charset=UTF-8");
        send(ex,status,body);
    }

    private static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] b=body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status,b.length);
        try(OutputStream out=ex.getResponseBody()){out.write(b);}
    }

    private static final String PAGE = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>StayHub - House Rental</title>
<style>
*{box-sizing:border-box}
body{margin:0;font-family:Arial,Helvetica,sans-serif;background:#f5f7fb;color:#111827}
nav{height:82px;background:#fff;border-bottom:1px solid #e5e7eb;display:flex;align-items:center;justify-content:space-between;padding:0 40px;position:sticky;top:0;z-index:10}
.logo{font-size:28px;font-weight:800;color:#1769e0;cursor:pointer}
.navlinks{display:flex;align-items:center;gap:18px}
.navlinks a{color:#374151;text-decoration:none;font-size:16px;cursor:pointer}
.btn{border:0;border-radius:6px;padding:11px 19px;font-size:15px;font-weight:600;cursor:pointer}
.btn-primary{background:#2867df;color:white}.btn-outline{background:#fff;border:1px solid #1769e0;color:#1769e0}
.btn-danger{background:#df3947;color:#fff}.btn-light{background:#fff;color:#374151;border:1px solid #e5e7eb}
.page{min-height:calc(100vh - 82px);padding:36px 9%}
.auth-wrap{min-height:calc(100vh - 82px);display:flex;justify-content:center;align-items:center;padding:35px}
.auth-card{background:white;width:430px;padding:38px 32px;border:1px solid #e1e5eb;border-radius:16px;box-shadow:0 5px 18px rgba(0,0,0,.06);text-align:center}
.lock{width:60px;height:60px;border-radius:15px;background:#eff6ff;margin:0 auto 16px;display:flex;align-items:center;justify-content:center;font-size:30px}
h1,h2,h3{margin-top:0}.auth-card h2{font-size:27px;margin:8px 0 25px}
input,select,textarea{width:100%;padding:14px;border:1px solid #d3d8df;background:#f9fafc;border-radius:8px;font-size:15px;outline:none}
input:focus,select:focus,textarea:focus{border-color:#2867df;background:#fff}
.field{margin-bottom:15px;text-align:left}.field label{display:block;margin-bottom:7px;color:#4b5563;font-size:14px;font-weight:600}
.auth-card .btn{width:100%;height:48px}.switch{margin-top:22px;color:#6b7280}.switch a{color:#1e5fbd;font-weight:600;cursor:pointer}
.hero{background:white;border-radius:18px;padding:40px;display:grid;grid-template-columns:1.15fr .85fr;gap:35px;align-items:center;border:1px solid #e4e8ee}
.hero h1{font-size:44px;line-height:1.08;margin-bottom:15px}.hero p{font-size:17px;color:#6b7280;line-height:1.7}
.hero img{width:100%;height:310px;object-fit:cover;border-radius:14px}
.searchbar{background:#fff;border:1px solid #e5e7eb;border-radius:12px;padding:15px;display:flex;gap:10px;margin:25px 0}
.searchbar input{flex:1}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:22px}
.card{background:#fff;border:1px solid #e2e6eb;border-radius:13px;overflow:hidden;box-shadow:0 3px 12px rgba(0,0,0,.04)}
.card img{width:100%;height:200px;object-fit:cover}.card-body{padding:18px}.card h3{font-size:19px;margin-bottom:8px}.muted{color:#6b7280}.price{font-size:24px;font-weight:800;margin:10px 0}.small{font-size:13px}
.dashboard-title{margin-bottom:25px}.dashboard-title p{color:#6b7280;margin-top:4px}
.tabs{display:flex;background:#fff;border:1px solid #e2e6eb;border-radius:9px;width:max-content;overflow:hidden;margin-bottom:28px}.tab{padding:12px 25px;cursor:pointer;color:#6b7280}.tab.active{background:#2867df;color:#fff}
.panel{background:#fff;border:1px solid #e1e5eb;border-radius:16px;padding:34px;margin-bottom:28px}
.panel h2{text-align:center;margin-bottom:30px}.form-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:20px}.full{grid-column:1/-1}
.form-actions{text-align:right;margin-top:20px}
.detail{background:#fff;border:1px solid #e1e5eb;border-radius:17px;padding:35px;display:grid;grid-template-columns:1fr 1fr;gap:25px}.detail img{width:100%;height:450px;object-fit:cover;border-radius:12px}.detail h1{font-size:30px}.detail .location{color:#6b7280;margin:12px 0 22px}.detail .price{font-size:28px}.detail hr{border:0;border-top:1px solid #e5e7eb;margin:25px 0}
.table-wrap{background:#fff;border:1px solid #e1e5eb;border-radius:13px;overflow:auto}.table{width:100%;border-collapse:collapse}.table th{background:#2867df;color:#fff;text-align:left;padding:15px}.table td{padding:16px;border-bottom:1px solid #e8ebef;color:#596273}.status{font-weight:700}.approved{color:#159447}.pending{color:#d58a00}.rejected{color:#dc3545}
.empty{padding:35px;text-align:center;color:#6b7280}
.toast{position:fixed;right:25px;bottom:25px;background:#111827;color:white;padding:15px 20px;border-radius:9px;display:none;z-index:100;box-shadow:0 8px 25px rgba(0,0,0,.2)}
.modal{position:fixed;inset:0;background:rgba(15,23,42,.55);display:none;align-items:center;justify-content:center;z-index:50}.modal-box{background:#fff;width:450px;border-radius:14px;padding:28px}.modal-actions{display:flex;gap:10px;justify-content:flex-end;margin-top:20px}
@media(max-width:800px){nav{padding:0 18px}.navlinks{gap:8px}.page{padding:25px 5%}.hero,.detail{grid-template-columns:1fr}.form-grid{grid-template-columns:1fr}.hero h1{font-size:34px}.auth-card{width:100%;max-width:430px}}
</style>
</head>
<body>
<nav>
  <div class="logo" onclick="showHome()">StayHub</div>
  <div class="navlinks" id="navlinks"></div>
</nav>
<div id="app"></div>
<div id="toast" class="toast"></div>

<script>
let currentUser = JSON.parse(localStorage.getItem("stayhubUser") || "null");
let properties = [];
let selectedProperty = null;

const imgFallback = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=1000&q=85";

function escapeHtml(v){return String(v??"").replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]));}
function money(n){return "₹"+Number(n||0).toLocaleString("en-IN");}
function statusLabel(s){
  if(s === "approved") return "Request Accepted";
  if(s === "rejected") return "Request Rejected";
  return "Request Pending";
}
function toast(msg){let t=document.getElementById("toast");t.textContent=msg;t.style.display="block";setTimeout(()=>t.style.display="none",3000);}
function setUser(u){currentUser=u;localStorage.setItem("stayhubUser",JSON.stringify(u));renderNav();}
function logout(){localStorage.removeItem("stayhubUser");currentUser=null;showLogin();}
function renderNav(){
  const n=document.getElementById("navlinks");
  if(currentUser){
    n.innerHTML=`<a onclick="showHome()">Home</a>
      <a onclick="${currentUser.role==='owner'?'showOwner()':'showRenter()'}">${currentUser.role==='owner'?'Owner Dashboard':'Renter Dashboard'}</a>
      <span class="muted">Hi, ${escapeHtml(currentUser.name)}</span>
      <button class="btn btn-danger" onclick="logout()">Log Out</button>`;
  }else{
    n.innerHTML=`<a onclick="showHome()">Home</a><button class="btn btn-outline" onclick="showLogin()">Login</button><button class="btn btn-primary" onclick="showRegister()">Register</button>`;
  }
}
async function api(url,opts={}){
  const r=await fetch(url,opts); const data=await r.json().catch(()=>({}));
  if(!r.ok) throw new Error(data.message||"Request failed"); return data;
}
function formBody(obj){return Object.entries(obj).map(([k,v])=>encodeURIComponent(k)+"="+encodeURIComponent(v??"")).join("&");}

async function loadProperties(){
  const data=await api("/api/properties"); properties=data; return data;
}
function showLogin(){
  renderNav();
  document.getElementById("app").innerHTML=`<div class="auth-wrap"><div class="auth-card">
    <div class="lock">🔐</div><h2>Sign In</h2>
    <form onsubmit="login(event)">
      <div class="field"><input id="email" type="email" placeholder="Email Address" required></div>
      <div class="field"><input id="password" type="password" placeholder="Password" required></div>
      <button class="btn btn-primary">Sign In</button>
    </form>
    <div class="switch">Don't have an account? <a onclick="showRegister()">Sign Up</a></div>
  </div></div>`;
}
async function login(e){
  e.preventDefault();
  const b=formBody({email:email.value,password:password.value});
  try{const d=await api("/api/login",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:b});setUser(d.user);toast("Signed in successfully");d.user.role==="owner"?showOwner():showRenter();}
  catch(err){toast(err.message);}
}
function showRegister(){
  renderNav();
  document.getElementById("app").innerHTML=`<div class="auth-wrap"><div class="auth-card">
    <div class="lock">📝</div><h2>Sign Up</h2>
    <form onsubmit="register(event)">
      <div class="field"><input id="rname" placeholder="Full Name" required></div>
      <div class="field"><input id="remail" type="email" placeholder="Email Address" required></div>
      <div class="field"><input id="rpass" type="password" placeholder="Password" required></div>
      <div class="field"><select id="role" required><option value="">Select User Type</option><option value="owner">Owner</option><option value="renter">Renter</option></select></div>
      <button class="btn btn-primary">Sign Up</button>
    </form>
    <div class="switch">Have an account? <a onclick="showLogin()">Sign In</a></div>
  </div></div>`;
}
async function register(e){
  e.preventDefault();
  try{
    const d=await api("/api/register",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},
      body:formBody({name:rname.value,email:remail.value,password:rpass.value,role:role.value})});
    toast(d.message); showLogin();
  }catch(err){toast(err.message);}
}
async function showHome(){
  renderNav();
  const ps=await loadProperties();
  document.getElementById("app").innerHTML=`<main class="page">
    <section class="hero">
      <div><div class="small muted">HOUSE RENTAL PLATFORM</div><h1>Find a place you can call home.</h1>
      <p>StayHub connects renters with owners and makes finding, listing and requesting rental homes simple.</p>
      <button class="btn btn-primary" onclick="document.getElementById('listing').scrollIntoView()">Explore Properties</button></div>
      <img src="${ps[0]?.image||imgFallback}" onerror="this.src='${imgFallback}'">
    </section>
    <div id="listing">
      <h2 style="margin-top:40px">Available Properties</h2>
      <div class="searchbar"><input id="search" placeholder="Search by location or property type..." oninput="filterProperties()"><button class="btn btn-primary" onclick="filterProperties()">Search</button></div>
      <div id="propertyGrid" class="grid"></div>
    </div>
  </main>`;
  renderPropertyCards(ps);
}
function renderPropertyCards(ps){
  document.getElementById("propertyGrid").innerHTML=ps.length?ps.map(p=>`<div class="card">
    <img src="${escapeHtml(p.image)}" onerror="this.src='${imgFallback}'">
    <div class="card-body"><div class="small muted">${escapeHtml(p.type)} • ${escapeHtml(p.adType)}</div>
    <h3>${escapeHtml(p.address.split(",").slice(0,2).join(","))}</h3>
    <div class="price">${money(p.amount)} <span class="small muted">/ month</span></div>
    <p class="muted small">${escapeHtml(p.details.substring(0,105))}...</p>
    <button class="btn btn-primary" onclick="showProperty(${p.id})">View Property</button></div></div>`).join(""):`<div class="empty">No properties found.</div>`;
}
function filterProperties(){
  const q=(document.getElementById("search")?.value||"").toLowerCase();
  renderPropertyCards(properties.filter(p=>(p.address+" "+p.type+" "+p.details).toLowerCase().includes(q)));
}
async function showProperty(id){
  if(!properties.length) await loadProperties();
  selectedProperty=properties.find(p=>p.id==id); if(!selectedProperty)return;
  renderNav();
  document.getElementById("app").innerHTML=`<main class="page"><div class="detail">
    <div><img src="${escapeHtml(selectedProperty.image)}" onerror="this.src='${imgFallback}'"></div>
    <div><h1>${escapeHtml(selectedProperty.type)} in ${escapeHtml(selectedProperty.address.split(",")[0])}</h1>
      <div class="location">📍 ${escapeHtml(selectedProperty.address)}</div>
      <div class="price">${money(selectedProperty.amount)}<span class="small muted"> / month</span></div>
      <p style="line-height:1.7;color:#4b5563">${escapeHtml(selectedProperty.details)}</p>
      <p><b>Property Type:</b> ${escapeHtml(selectedProperty.type)} • <b>Ad Type:</b> ${escapeHtml(selectedProperty.adType)}</p>
      <hr>
      <h3>Send booking request</h3>
      ${currentUser?.role==='renter'?`<form onsubmit="requestRental(event)">
        <div class="field"><label>Move-in Date</label><input id="moveDate" type="date" required></div>
        <div class="field"><label>Your Contact Number</label><input id="phone" placeholder="Enter your phone number" required></div>
        <div class="field"><label>Message to owner</label><textarea id="message" rows="4" placeholder="Message to owner..."></textarea></div>
        <button id="reqBtn" class="btn btn-primary" style="width:100%">Request Rental</button>
      </form>`:`<p class="muted">Please <a onclick="showLogin()" style="color:#1769e0;cursor:pointer">sign in as a renter</a> to request this property.</p>`}
      <div style="text-align:center;margin-top:22px"><a onclick="showHome()" style="color:#285fae;cursor:pointer">Back to properties</a></div>
    </div>
  </div></main>`;
}
async function requestRental(e){
  e.preventDefault();
  const btn=document.getElementById("reqBtn");btn.disabled=true;btn.textContent="Sending request...";
  try{
    const d=await api("/api/bookings",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},
      body:formBody({propertyId:selectedProperty.id,renterId:currentUser.id,phone:phone.value,moveInDate:moveDate.value,message:message.value})});
    alert(d.message); showRenter();
  }catch(err){toast(err.message);btn.disabled=false;btn.textContent="Request Rental";}
}
async function showOwner(){
  renderNav(); await loadProperties();
  const own=properties.filter(p=>p.ownerId===currentUser.id);
  document.getElementById("app").innerHTML=`<main class="page"><div class="dashboard-title"><h1>Owner Dashboard</h1><p>Welcome back, <b>${escapeHtml(currentUser.name)}</b></p></div>
    <div class="tabs"><div class="tab active" onclick="ownerTab('add')">Add Property</div><div class="tab" onclick="ownerTab('properties')">All Properties (${own.length})</div><div class="tab" onclick="ownerTab('bookings')">All Bookings</div></div>
    <div id="ownerContent"></div></main>`;
  ownerTab("add");
}
async function ownerTab(tab){
  document.querySelectorAll(".tab").forEach(x=>x.classList.remove("active"));
  const tabs=document.querySelectorAll(".tab"); if(tab==="add")tabs[0]?.classList.add("active"); if(tab==="properties")tabs[1]?.classList.add("active");if(tab==="bookings")tabs[2]?.classList.add("active");
  const c=document.getElementById("ownerContent");
  if(tab==="add"){
    c.innerHTML=`<div class="panel"><h2>Add New Property</h2><form onsubmit="addProperty(event)">
      <div class="form-grid">
        <div class="field"><label>Property Type</label><select id="ptype"><option>Residential</option><option>Commercial</option></select></div>
        <div class="field"><label>Property Ad Type</label><select id="adtype"><option>Rent</option><option>Sale</option></select></div>
        <div class="field"><label>Property Full Address</label><input id="address" placeholder="Flat 503, Silver Oak Towers, Wakad - Hinjewadi..." required></div>
        <div class="field"><label>Property Image URL</label><input id="pimage" placeholder="Paste house image URL (optional)"></div>
        <div class="field"><label>Owner Contact No.</label><input id="contact" placeholder="Contact number" required></div>
        <div class="field"><label>Property Amount</label><input id="amount" type="number" placeholder="25000" required></div>
        <div class="field full"><label>Additional Details for the Property</label><textarea id="details" rows="4" placeholder="Add any details here..." required></textarea></div>
      </div><div class="form-actions"><button class="btn btn-primary">Submit Form</button></div>
    </form></div>`;
  }else if(tab==="properties"){
    await loadProperties(); const own=properties.filter(p=>p.ownerId===currentUser.id);
    c.innerHTML=own.length?`<div class="grid">${own.map(p=>`<div class="card"><img src="${escapeHtml(p.image)}" onerror="this.src='${imgFallback}'"><div class="card-body"><h3>${escapeHtml(p.address.split(",")[0])}</h3><div class="price">${money(p.amount)}</div><p class="small muted">${escapeHtml(p.details.substring(0,100))}...</p></div></div>`).join("")}</div>`:`<div class="panel empty">You have not added any properties yet.</div>`;
  }else{
    const data=await api("/api/bookings?ownerId="+encodeURIComponent(currentUser.id));
    c.innerHTML=data.length?`<div class="table-wrap"><table class="table"><tr><th>Booking ID</th><th>Property</th><th>Tenant Name</th><th>Phone</th><th>Move-in</th><th>Status</th><th>Action</th></tr>
      ${data.map(b=>`<tr><td>${b.id}</td><td>${escapeHtml(b.propertyAddress)}</td><td>${escapeHtml(b.renterName)}</td><td>${escapeHtml(b.phone)}</td><td>${escapeHtml(b.moveInDate)}</td><td class="status ${b.status}">${statusLabel(b.status)}</td><td>${b.status==="pending"?`<button class="btn btn-primary small" onclick="updateBooking(${b.id},'approved')">Approve</button> <button class="btn btn-danger small" onclick="updateBooking(${b.id},'rejected')">Reject</button>`:"—"}</td></tr>`).join("")}</table></div>`:`<div class="panel empty">No booking requests yet.</div>`;
  }
}
async function addProperty(e){
  e.preventDefault();
  try{
    const d=await api("/api/properties",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},
      body:formBody({ownerId:currentUser.id,type:ptype.value,adType:adtype.value,address:address.value,contact:contact.value,amount:amount.value,details:details.value,image:pimage.value})});
    toast(d.message); await showOwner();
  }catch(err){toast(err.message);}
}
async function updateBooking(id,status){
  try{
    const d=await api("/api/bookings/status",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:formBody({bookingId:id,status})});
    toast(status === "approved" ? "Request accepted. The renter can now see Request Accepted." : "Request rejected. The renter can now see Request Rejected.");
    await showOwner();
    await ownerTab("bookings");
  }catch(err){toast(err.message);}
}
async function showRenter(){
  renderNav();
  document.getElementById("app").innerHTML=`<main class="page"><div class="dashboard-title"><h1>Renter Dashboard</h1><p>Welcome back, <b>${escapeHtml(currentUser.name)}</b></p></div>
    <div class="tabs"><div class="tab active" onclick="renterTab('all')">All Properties</div><div class="tab" onclick="renterTab('history')">Booking History</div></div>
    <div id="renterContent"></div></main>`;
  renterTab("all");
}
async function renterTab(tab){
  document.querySelectorAll(".tab").forEach(x=>x.classList.remove("active"));
  const tabs=document.querySelectorAll(".tab"); if(tab==="all")tabs[0]?.classList.add("active");else tabs[1]?.classList.add("active");
  const c=document.getElementById("renterContent");
  if(tab==="all"){await loadProperties();renderRenterCards(properties);}
  else{
    const data=await api("/api/bookings?renterId="+encodeURIComponent(currentUser.id));
    c.innerHTML=`<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:15px"><h2 style="margin:0">My Rental Requests</h2><button class="btn btn-light" onclick="renterTab('history')">↻ Refresh Status</button></div>` + (data.length?`<div class="table-wrap"><table class="table"><tr><th>Booking ID</th><th>Property ID</th><th>Property</th><th>Phone</th><th>Status</th></tr>
      ${data.map(b=>`<tr><td>${b.id}</td><td>${b.propertyId}</td><td>${escapeHtml(b.propertyAddress)}</td><td>${escapeHtml(b.phone)}</td><td class="status ${b.status}">${escapeHtml(b.status)}</td></tr>`).join("")}</table></div>`:`<div class="panel empty">No bookings yet.</div>`);
  }
}
function renderRenterCards(ps){
  document.getElementById("renterContent").innerHTML=`<div class="grid">${ps.map(p=>`<div class="card"><img src="${escapeHtml(p.image)}" onerror="this.src='${imgFallback}'"><div class="card-body"><div class="small muted">${escapeHtml(p.type)} • ${escapeHtml(p.adType)}</div><h3>${escapeHtml(p.address.split(",")[0])}</h3><p class="small muted">📍 ${escapeHtml(p.address)}</p><div class="price">${money(p.amount)} <span class="small muted">/ month</span></div><button class="btn btn-primary" onclick="showProperty(${p.id})">View & Request</button></div></div>`).join("")}</div>`;
}
renderNav();
showHome();
</script>
</body>
</html>
""";
}
