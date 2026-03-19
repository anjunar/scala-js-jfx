package app

import jfx.action.Button
import jfx.core.state.Property
import jfx.form.{Form, Input}
import jfx.layout.Div
import jfx.statement.Conditional
import org.scalajs.dom

object Main {

  def main(args: Array[String]): Unit = {

    val person = Person(Property("John"), Property("Doe"))

    val opening = new Property(true)

    val div = new Div()

    val toggle = new Button()
    toggle.textContent = "Toggle"
    toggle.buttonType = "button"

    val form = new Form(person)
    val container = new Div()

    form.addChild(container)

    val firstNameInput = new Input("firstName")
    val lastNameInput = new Input("lastName")

    container.addChild(toggle)

    div.addChild(form)

    val conditional = new Conditional(opening)
    conditional.thenAdd(firstNameInput)
    conditional.thenAdd(lastNameInput)

    val div1 = new Div()
    div1.textContent = "Hello"
    conditional.elseAdd(div1)

    container.addChild(conditional)

    toggle.addClick(_ => opening.set(!opening.get))

    dom.document.body.appendChild(div.element)

  }
  
}
