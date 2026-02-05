ENDPOINTS


COMO PONER EN FUNCIONAMIENTO EN LOCAL
El primer paso para poner en funcionamiento este backend en nuestro ordenador de manera local es descargar el src que hay en el repositorio de github.
Una vez lo hayamos descargado deberemos dejar unas cuantas cosas declaradas antes de ejecutarlo, lo primero será añadir las dependencias a nuestro pom.xml.

Estas son las dependencias en cuestión que añadiremos. 

Estas son las dependencias que nos ayudará a ejecutar el proyecto como un backend
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        
Esta es la dependencia que nos ayudará a conectarnos con nuestra base de datos de FireStore
        <dependency>
            <groupId>com.google.firebase</groupId>
            <artifactId>firebase-admin</artifactId>
            <version>9.2.0</version>
        </dependency>

Esta es la dependencia que nos ayudará a tener más seguridad en nuestro backend pudiendo encriptar contraseñas.
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

Esta dependencia nos ayudará a integrar una caché en nuestro backend para que determinados endpoints se ejecuten más rápido
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>

Estas dependencias son las que nos ayudarán a generar los tokens que permitirán a los usuarios hacer distintas operaciones
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
Estas dependencias se pueden copiar y pegar en el pom.xml y al recargarlo se habrán instalado las dependencias.

Una vez hayamos instalado las dependencias crearemos un proyecto de firebase,
        
