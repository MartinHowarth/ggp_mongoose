package org.ggp.base.util.propnet.architecture.components;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Proposition class is designed to represent named latches.
 */
@SuppressWarnings("serial")
public class Proposition extends Component
{
	/** The name of the Proposition. */
	private GdlSentence name;
	/** The value of the Proposition. */
	private boolean value;

	// TODO move this to separate GoalProposition. Couldn't get it to drop in nicely elsewhere, so got lazy.
	public Integer goal;

	/**
	 * Creates a new Proposition with name <tt>name</tt>.
	 *
	 * @param name
	 *            The name of the Proposition.
	 */
	public Proposition(GdlSentence name)
	{
		this.name = name;
		this.value = false;
	}

	/**
	 * Getter method.
	 *
	 * @return The name of the Proposition.
	 */
	public GdlSentence getName()
	{
		return name;
	}

    /**
     * Setter method.
     *
     * This should only be rarely used; the name of a proposition
     * is usually constant over its entire lifetime.
     *
     * @return The name of the Proposition.
     */
    public void setName(GdlSentence newName)
    {
        name = newName;
    }

	/**
	 * Returns the current value of the Proposition.
	 *
	 * @see org.ggp.base.util.propnet.architecture.Component#getValue()
	 */
	@Override
	public boolean getValue()
	{
		return value;
	}

	/**
	 * Setter method.
	 *
	 * @param value
	 *            The new value of the Proposition.
	 */
	public void setValue(boolean value)
	{
		this.value = value;
	}

	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#toString()
	 */

    public void calculateGoal() {
        GdlRelation relation = (GdlRelation) getName();
        GdlConstant constant = (GdlConstant) relation.get(1);
        goal = Integer.parseInt(constant.toString());
    }

	@Override
	public String toString()
	{
		return toDot("circle", value ? "red" : "white", name.toString());
	}
}