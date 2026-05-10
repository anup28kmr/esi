================================================================================
DEBUGGING GUIDE: Tracing Issues in the Signup Flow
================================================================================

This guide helps you debug issues at each stage of the signup flow.

================================================================================
STAGE 1: FRONTEND FORM SUBMISSION
================================================================================

Problem: "Form submits but nothing happens"

CHECK:
1. Browser Console for errors
   └─ Open DevTools (F12) → Console tab
   └─ Look for JavaScript errors
   └─ Should see: "[SignupView] onSubmit called"

2. Check Form Validation
   └─ Email field: should have email format
   └─ Password field: minimum 8 characters (line 11)
   └─ If validation fails, button click doesn't trigger submit

3. Form Data
   └─ Open DevTools → Console
   └─ Look for: "[SignupView] Form data: {email: ..., role: ...}"

DEBUG:
┌─ Add console logs in SignupView.vue Line 48:
│  console.log('[SignupView] onSubmit called');
│  console.log('[SignupView] email=' + this.email);
│  console.log('[SignupView] password length=' + this.password.length);
│  console.log('[SignupView] role=' + this.role);
└─ Watch for these logs in browser console

================================================================================
STAGE 2: FRONTEND → API CLIENT
================================================================================

Problem: "Form submits but shows network error"

CHECK:
1. Browser DevTools → Network Tab
   └─ Look for OPTIONS request to /api/users
   └─ If no request appears, error is in client.js
   └─ If OPTIONS shows 404, error is in gateway routing

2. API Client Logs
   └─ Browser Console should show:
      └─ "[API Client] Module loaded"
      └─ "[API Client] BASE_URL: http://localhost:9090"
      └─ "[API Client] buildUrl called with path: /api/users"
      └─ "[API Client] Full URL: http://localhost:9090/api/users"
      └─ "[API Client] Calling fetch with URL: http://localhost:9090/api/users"

3. CORS Preflight
   └─ Network Tab → OPTIONS request
   └─ Look at Response Headers:
      └─ Access-Control-Allow-Origin: should be present
      └─ Access-Control-Allow-Methods: should include POST
   └─ If headers missing → Gateway CORS config issue
   └─ If origin doesn't match → check CorsConfig allowedOrigins list

DEBUG:
┌─ Verify BASE_URL is correct
│  └─ In browser console, run: console.log(api.baseUrl)
│  └─ Should output: "http://localhost:9090"
│
├─ Check network request URL
│  └─ DevTools → Network → click OPTIONS request
│  └─ Check "Request URL" = "http://localhost:9090/api/users?..." ✓
│
└─ Verify CORS headers in OPTIONS response
   └─ DevTools → Network → OPTIONS → Response Headers
   └─ Look for: Access-Control-Allow-Origin: http://localhost:8090

LOGS TO CHECK:
├─ Gateway logs: docker compose logs api-gateway
│  └─ Look for: "API gateway request started: method=OPTIONS, path=/api/users"
│
└─ No errors = gateway is working

================================================================================
STAGE 3: API GATEWAY - REQUEST RECEIVED
================================================================================

Problem: "Network request succeeds but returns 404"

CHECK GATEWAY LOGS:
docker compose logs api-gateway --tail 50

Look for these log lines:

Line 1: "API gateway request started: method=OPTIONS, path=/api/users"
└─ This is the CORS preflight
└─ Should see this before the POST request

Line 2: "API gateway request started: method=POST, path=/api/users"
└─ Actual POST request
└─ If missing → request never reached gateway

Line 3: "Gateway controller intercepted: method=POST, path=/api/users"
└─ ApiGatewayController was invoked
└─ If missing → routing failed

Line 4: "Routing to user-service: http://user-service:7000/users"
└─ Path rewrite successful
└─ Shows target URL being called

Line 5: "Forwarding request: method=POST, url=http://user-service:7000/users"
└─ RestTemplate about to send request downstream

Line 6: "API gateway request completed: method=POST, status=201"
└─ Response received from user-service
└─ Status 201 = success ✓
└─ Status 500 = user-service error
└─ Status 503 = user-service unavailable

DEBUG:
┌─ If you see "API gateway request started" but no "Gateway controller intercepted"
│  └─ Problem: CORS or routing failure
│  └─ Check: CorsConfig or @RequestMapping("/api")
│
├─ If you see "Gateway controller intercepted" but no "Routing to user-service"
│  └─ Problem: Path doesn't match conditions
│  └─ Check: if (path.contains("/users")) condition
│  └─ Current path may not contain "/users"
│
├─ If you see "Routing to user-service" but no "Forwarding request"
│  └─ Problem: Exception in routeToUserService()
│  └─ Check: path rewriting logic (replaceFirst)
│
└─ If you see "Forwarding request" but no response
   └─ Problem: User-service connection failed
   └─ Check: Is user-service running?
   └─ Check: Can gateway reach user-service?

TEST CONNECTIVITY:
From your machine:
┌─ Docker → exec into gateway:
│  docker compose exec api-gateway sh
│  ping user-service  # Should work
│  curl http://user-service:7000/users  # Should fail with 404 (no GET at root)
│
└─ Or directly test gateway:
   curl -X POST http://localhost:9090/api/users \
     -H "Content-Type: application/json" \
     -d '{"email":"test@test.com","password":"test1234","role":"Customer"}'

================================================================================
STAGE 4: API GATEWAY - ROUTING LOGIC
================================================================================

Problem: "Gateway says 'No route found for path'"

CHECK LOGS:
├─ If you see: "No route found for path: /api/users"
│  └─ Problem: None of the if conditions matched in line 47-61
│  └─ path = "/api/users"
│  └─ Check passes: path.contains("/users") is TRUE
│  └─ So shouldn't reach "No route found"
│  └─ This suggests code wasn't updated
│
└─ If you see this with different path:
   └─ Check what path is being sent
   └─ Example: /api/orders → should route to order-service
   └─ Example: /api/restaurants → should route to restaurant-service

DEBUG:
┌─ Find the exact failure point in logs
│  └─ Search for: "Gateway controller intercepted"
│  └─ Line right after shows method and path
│  └─ Confirm path is what you expect
│
├─ Verify path contains correct substring
│  └─ path.contains("/users") checks if string contains substring
│  └─ Path "/api/users" contains "/users" ✓
│  └─ Path "/api/user/123" contains "/users"? NO ✗ (no 's')
│
└─ Check for typos in path

FORCE TRACE:
Add temporary logging in gateway:
┌─ Edit: ApiGatewayController.java
│  Add after line 43:
│  log.info("Path analysis: path={}, contains /users={}, contains /auth={}",
│      path, path.contains("/users"), path.contains("/auth"));
│
├─ Recompile: mvn compile
├─ Rebuild Docker: docker compose build api-gateway
├─ Restart: docker compose up -d api-gateway
├─ Check logs: docker compose logs api-gateway
│
└─ Will see detailed breakdown of path matching

================================================================================
STAGE 5: API GATEWAY - ROUTING TO USER SERVICE
================================================================================

Problem: "Gets to gateway but user-service returns error"

CHECK USER-SERVICE LOGS:
docker compose logs user-service --tail 50

Look for:
├─ "Register user request received, email=user@example.com" 
│  └─ Controller method was called ✓
│
├─ "Register user completed, email=..., role=..."
│  └─ Success! User was created
│
├─ "WARN ..." or "ERROR ..."
│  └─ Check error message
│  └─ Common: "Email already exists"
│  └─ Common: "Password validation failed"
│  └─ Common: "Database connection error"
│
└─ No "Register user request received"
   └─ Request never reached controller
   └─ Check: Was path rewritten correctly?

CHECK PATH REWRITING:
Gateway logs should show:
└─ "Routing to user-service: http://user-service:7000/users"

If this shows wrong path:
└─ Problem in line 80: targetPath = originalPath.replaceFirst("/api", "");
└─ Debug:
   ├─ Add log: log.debug("Path rewriting: {} → {}", originalPath, targetPath);
   ├─ If targetPath = "/api/users" (still has /api)
   ├─ Then replaceFirst didn't work (check for regex issues)
   └─ If targetPath = "/" (removed too much)
      └─ Problem: replaceFirst matching on regex characters

TEST PATH DIRECTLY:
┌─ Exec into gateway container:
│  docker compose exec api-gateway sh
│
├─ Test path from inside container:
│  curl -X POST http://user-service:7000/users \
│    -H "Content-Type: application/json" \
│    -d '{"email":"test@test.com","password":"test1234","role":"Customer"}'
│
└─ If successful: container networking works ✓
   If failed: check user-service logs for why it rejected the request

================================================================================
STAGE 6: USER SERVICE - PROCESSING
================================================================================

Problem: "User-service returns error"

CHECK USER-SERVICE LOGS:
docker compose logs user-service --tail 50

Common errors:

1. "Email already exists"
   └─ UserDTO validation failed
   └─ User already registered with that email
   └─ Solution: Use different email

2. "Password validation failed"
   └─ Usually: too short (< 8 chars in SignupView)
   └─ Solution: Check password field validation in SignupView.vue line 11

3. "Database error: connection refused"
   └─ PostgreSQL container not running
   └─ Solution: docker compose up -d postgres

4. "Field 'email' cannot be null"
   └─ Email field missing from request body
   └─ Check: Is POST body correct format?
   └─ Check: Did gateway pass body correctly?

5. "Invalid JSON"
   └─ RequestBody deserialization failed
   └─ Check: Content-Type header is application/json?
   └─ Check: Valid JSON in request body (no syntax errors)?

DEBUG:
┌─ Look at user-service source code
│  File: user-service/src/main/java/.../UserController.java
│  Line 69: log.info("Register user request received, email={}", userDTO.email());
│
├─ If this log appears with wrong email value
│  └─ Problem: JSON deserialization error
│  └─ Check: Request body format
│
├─ Add more detailed logging:
│  log.info("UserDTO fields: email={}, password_length={}, role={}", 
│      userDTO.email(), userDTO.password().length(), userDTO.role());
│
└─ Recompile and rebuild user-service

USEFUL CONTAINER COMMANDS:
docker compose exec user-service sh
├─ Inside container, you can:
│  ├─ Check PostgreSQL connection:
│  │  psql postgresql://user:pass@postgres:5432/quickbite
│  │  \dt  # List tables in database
│  │  SELECT * FROM users;  # View registered users
│  │
│  ├─ Check logs while running:
│  │  # Logs already visible outside, but can check app logs:
│  │  tail -f /logs/app.log  (if configured)
│  │
│  └─ Check JAR file:
│     ls -lah /app/app.jar

================================================================================
STAGE 7: USER SERVICE → GATEWAY RESPONSE
================================================================================

Problem: "User-service responds but gateway returns error"

CHECK GATEWAY LOGS:
docker compose logs api-gateway --tail 20

Look for:
├─ Success case:
│  └─ "API gateway request completed: method=POST, status=201, ..."
│  └─ Should also show: "routeId=user-service-api-users"
│  └─ And: "targetUri=http://user-service:7000/users"
│
└─ Error case:
   └─ "API gateway request completed: method=POST, status=500, ..."
   └─ Check error message in logs

If status is not 201:
├─ Status 500: Internal error in gateway
│  └─ Check: RestTemplate exception in logs
│  └─ Check: forwardRequest() line 174 
│
└─ Status 400: Bad request
   └─ User-service rejected the data
   └─ Check: User-service logs for validation errors

================================================================================
STAGE 8: GATEWAY → FRONTEND RESPONSE
================================================================================

Problem: "Frontend shows error even though gateway returned 201"

CHECK BROWSER LOGS:
├─ "[API Client] Response status: 201"
│  └─ Response received correctly
│
├─ "[API Client] Parsed response: {id:..., email:..., ...}"
│  └─ JSON parsed successfully
│
├─ "[API Client] Request successful, returning: {...}"
│  └─ API client returned result to Vue component
│
└─ "[SignupView] Signup successful, result: {...}"
   └─ Vue component received result

If you see errors in console:
├─ CORS error (but shouldn't get here if we pass OPTIONS)
│  └─ Check: Response headers have Access-Control-Allow-Origin?
│
├─ JSON parsing error
│  └─ Check: Response body is valid JSON
│  └─ Open DevTools → Network → click request
│  └─ View the raw response body
│
└─ Other JavaScript error
   └─ Check: Full error message in console

VERIFY CORS RESPONSE:
DevTools → Network → Click POST request:
├─ Response Headers tab:
│  ├─ Content-Type: application/json ✓
│  ├─ Access-Control-Allow-Origin: http://localhost:8090 ✓
│  └─ Should NOT have: Access-Control-Allow-Origin: *
│     (specific origin is required for credentials: true)
│
└─ If headers missing:
   └─ Problem: Gateway CORS config
   └─ Check: CorsConfig.java allowedOrigins

================================================================================
STAGE 9: FRONTEND - UI UPDATE
================================================================================

Problem: "Page doesn't navigate to login after successful signup"

CHECK BROWSER LOGS:
├─ Should see: "[SignupView] Signup successful, result: {...}"
│  └─ If missing: Vue didn't receive response
│  └─ If present: Vue received response but navigation failed
│
└─ Check: Router configuration

VERIFY ROUTER:
File: frontend/src/router/index.js
├─ Should have route named 'login'
│  └─ Used in Line 60: this.$router.push({ name: 'login', ... })
│  └─ If name doesn't exist: Vue Router throws error in console
│
├─ Route should point to LoginView.vue
│
└─ Test: Manually navigate to /login in browser
   └─ If it works → router config is fine
   └─ If 404 → check router configuration

TEST NAVIGATION:
┌─ In browser DevTools console:
│  this.$router.push({ name: 'login', query: { registered: '1' } })
│  └─ If this throws error: router config problem
│  └─ If this works: component can navigate ✓
│
└─ Check Vue DevTools → Router History
   └─ Shows all navigation events
   └─ Can see if navigation was attempted

VERIFY REDIRECT:
└─ After successful signup, browser URL should change:
   ├─ FROM: http://localhost:8090/signup
   ├─ TO: http://localhost:8090/login?registered=1
   │
   └─ If no change:
      ├─ Check: this.$router.push() was called
      ├─ Check: No exception killed the function
      └─ Check: Browser DevTools application tab → cookies/storage

================================================================================
COMPLETE DEBUG FLOWCHART
================================================================================

START: User clicks "Create Account"
  ↓
[CHECK 1: Form Validation Error?]
  ├─ YES → Fix: Email format, password length
  └─ NO ↓
[CHECK 2: Browser Console Shows apiFetch Logs?]
  ├─ NO → Fix: CLIENT initialization issue, check dev server
  └─ YES ↓
[CHECK 3: Network Tab Shows OPTIONS Request?]
  ├─ NO → Fix: CORS error, check BASE_URL and allowed origins
  └─ YES ↓
[CHECK 4: OPTIONS Returns CORS Headers?]
  ├─ NO → Fix: Gateway CorsConfig
  └─ YES ↓
[CHECK 5: Network Tab Shows POST Request?]
  ├─ NO → Fix: Browser blocked request
  └─ YES ↓
[CHECK 6: POST Response Status 201?]
  ├─ NO → Fix: Gateway or User-Service error (see logs)
  └─ YES ↓
[CHECK 7: Browser Console Shows "Signup successful"?]
  ├─ NO → Fix: API Client response parsing error
  └─ YES ↓
[CHECK 8: URL Changed to /login?]
  ├─ NO → Fix: Vue Router configuration
  └─ YES ↓
✓ SUCCESS: Account created!

================================================================================
QUICK COMMAND REFERENCE
================================================================================

View All Logs (all containers):
└─ docker compose logs

View Gateway Logs (last 50 lines, follow new logs):
└─ docker compose logs api-gateway -f --tail 50

View User-Service Logs:
└─ docker compose logs user-service --tail 50

View PostgreSQL Connection Log:
└─ docker compose logs postgres --tail 20

Filter logs for specific text:
└─ docker compose logs api-gateway | grep "request"

Check if services are running:
└─ docker compose ps

Check service health:
└─ docker compose ps --filter "status=running"

Restart a service:
└─ docker compose restart api-gateway

Rebuild and restart:
└─ docker compose build api-gateway
└─ docker compose up -d api-gateway

View full Docker network:
└─ docker network ls
└─ docker network inspect quickbite (or project name)

Exec into container:
└─ docker compose exec user-service sh

Test HTTP from container:
└─ docker compose exec api-gateway curl http://user-service:7000/users

Follow logs real-time:
└─ docker compose logs -f

Clear logs (truncate):
└─ docker compose logs --no-log-prefix | wc -l

================================================================================

