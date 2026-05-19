# Architecture Decision Record (ADR)

## ADR 001: Centralized Stateless Authentication via Custom Asymmetric JWT Server

### Status
**Accepted**

### Context
We are designing a microservice-based architecture that requires robust, secure, and scalable user authentication and authorization across multiple distributed services (e.g., Order Service, Product Service, API Gateway). The architecture demands that individual microservices remain completely decoupled, scale horizontally without performance or storage bottlenecks, and avoid unnecessary external runtime dependencies or commercial licensing costs.

---

### Decision
We will implement a **custom-built, centralized Authentication Server** using **Spring Boot 3+** and **Spring Security 6+**. This server will issue stateless **JSON Web Tokens (JWT)** signed using an **Asymmetric Cryptography Pair (RSA-256)**. 

* **The Auth Server** will hold the **Private Key** securely and use it exclusively to sign outgoing tokens.
* **The Auth Server** will expose a public JSON Web Key Set (**JWKS**) endpoint at `/.well-known/jwks.json`.
* **Downstream microservices / API Gateways** will act as **OAuth2 Resource Servers**, dynamically consuming the JWKS endpoint to obtain the **Public Key** required for token signature validation.

---

### Rationale

We evaluated several alternative security architectures and rejected them based on our structural and operational goals:

#### 1. Why JWT over Session-Based Authentication?
* **The Session Problem (Stateful):** Session-based authentication requires the server to maintain user session state in memory or inside a shared database (like Redis). In a distributed microservice environment, this introduces tight coupling and shared data state dependencies. If a request hits `Service A`, it must make an I/O network call to a shared session store to verify if the user is authenticated. This creates a critical single point of failure and a massive performance bottleneck.
* **The JWT Solution (Stateless):** JWTs are completely self-contained. The token contains the user's identity, roles, and metadata (Claims). Microservices can validate the token's cryptographic signature entirely on their own, locally, without making a single database query or network trip. This ensures high throughput and infinite horizontal scaling capability for individual services.

#### 2. Why a Custom Server over Third-Party Identity Providers (e.g., Auth0, Okta, Firebase)?
* **Eliminating External Runtime Dependencies:** Utilizing a third-party SaaS provider introduces a high-risk runtime dependency. If the external provider experiences an outage, our entire ecosystem's authentication flow breaks.
* **Cost Predictability:** Third-party providers typically charge based on Monthly Active Users (MAU). As the application scales, subscription costs grow aggressively. A custom server runs on our infrastructure, making costs linear with computational resource provisioning.
* **Complete Architectural Control:** Building our own server ensures that our user data schemas, user registration workflows, data compliance policies, and custom business rules are entirely managed within our own private database infrastructure.

#### 3. Why Asymmetric (RSA) over Symmetric Cryptography?
* **Blast Radius Isolation:** In a symmetric setup, every microservice must know the exact same shared secret string to both sign and verify tokens. If an attacker breaches a single downstream microservice (e.g., a non-critical notification or catalog service), they can extract that shared secret. With the secret, the attacker can forge their own valid admin-level tokens and compromise the entire system.
* **Asymmetric Security Bounds:** With RSA, downstream microservices only ever hold the **Public Key**. They can read and verify tokens, but they are physically incapable of manufacturing or altering them. The highly critical **Private Key** never leaves the isolated, highly guarded boundary of the central Auth Server.

---

### Current Trade-offs & Challenges

While this approach matches our scaling goals, it introduces technical complexities that we must explicitly manage during development:

| Challenge | Architectural Impact | Strategy / Mitigation |
| :--- | :--- | :--- |
| **Token Revocation** | Because JWT verification is stateless, once a token is issued, it cannot be easily invalidated (e.g., if a user logs out, changes passwords, or is banned) without checking a database. | Keep Access Token lifespans brief (e.g., 15 minutes) and implement a secure, stateful **Refresh Token** rotation mechanism handled solely by the Auth Server to grant new short-lived access tokens. |
| **Development & Maintenance Overhead** | Custom implementations require us to manually manage cryptographic key loaders, token encoders, sign-up/login logic, and handle low-level Spring Security filter chain exceptions. | Use trusted, standard, underlying libraries like `Nimbus-JOSE-JWT` (the default stack inside Spring Security) rather than writing low-level cryptographic logic from scratch. |
| **Key Rotation Overhead** | If a Private Key is compromised or expires, manually updating a hardcoded public key across all downstream microservices would cause massive service disruption. | Expose a native `/.well-known/jwks.json` endpoint on the Auth Server. Downstream microservices will dynamically fetch and cache the public key. We can rotate keys on the Auth Server with zero downtime. |

---

### Verification & Success Criteria

To ensure that this architecture fulfills our performance and security expectations, the system must pass the following validation parameters:

1. **Network Isolation Validation:** Downstream microservices must successfully authenticate requests and extract roles (`SecurityContext`) even if their direct network access to the primary user database or the central Auth Server is completely disconnected at runtime.
2. **Performance Benchmarking:** Token parsing, cryptographic signature verification, and security context initialization on downstream resource servers must operate in the sub-millisecond range ($<1\text{ms}$), adding zero perceptible latency to business API endpoints.
3. **JWKS Caching and Efficiency:** Downstream microservices must not call the Auth Server's JWKS endpoint on every incoming request. The public keys must be cached locally by Spring Security's reactive framework and only re-fetched if an incoming token presents an unknown Key ID (`kid`).