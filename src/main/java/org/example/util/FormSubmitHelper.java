package org.example.util;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;

/**
 * Enlaza la tecla Enter en campos de texto con la acción principal del formulario.
 */
public final class FormSubmitHelper {

    private FormSubmitHelper() {
    }

    /**
     * Al pulsar Enter en cualquiera de los campos, ejecuta la acción del botón principal
     * (equivalente a un clic), si el botón no está deshabilitado.
     */
    public static void bindPrimaryAction(Button primaryButton, TextInputControl... fields) {
        if (primaryButton == null) {
            return;
        }

        Runnable submit = () -> {
            if (!primaryButton.isDisabled()) {
                primaryButton.fire();
            }
        };

        bindEnterAction(submit, fields);
        primaryButton.setDefaultButton(true);
    }

    /**
     * Ejecuta una acción personalizada al pulsar Enter en los campos indicados.
     * Compatible con TextField y PasswordField (hereda de TextField).
     */
    public static void bindEnterAction(Runnable action, TextInputControl... fields) {
        if (action == null || fields == null) {
            return;
        }
        for (TextInputControl field : fields) {
            if (field instanceof TextField textField) {
                textField.setOnAction(event -> action.run());
            }
        }
    }
}

