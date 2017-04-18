#ifndef LINPROG_SOLVER_H
#define LINPROG_SOLVER_H

#include "linprog/model.h"

class Solution
{

public:
	virtual ~Solution() {};

	virtual double get_value(unsigned int variable_index) const = 0;

	virtual double evaluate(const LinearExpression &exp) const
	{
		double sum = 0;
		foreach(exp.get_terms(), term)
		{
			double coeff     = term->first;
			unsigned int var = term->second;
			sum += coeff * get_value(var);

		
		}
		return sum;
	}

	virtual double evaluateSpin(const LinearExpression &exp) const
	{
		double sum = 0;
		int index = 0;
		foreach(exp.get_terms(), term)
		{
			double coeff     = term->first;
			unsigned int var = term->second;
			if(index %2 == 0)
				sum+= coeff * get_value(var);
			index++;
		}
		return sum;
	}
	virtual double evaluateArrival(const LinearExpression &exp) const
	{
		double sum = 0;
		int index = 0;
		foreach(exp.get_terms(), term)
		{
			double coeff     = term->first;
			unsigned int var = term->second;
			if(index %2 == 1)
				sum+= coeff * get_value(var);
			index++;
		}
		return sum;
	}
};

#if defined(CONFIG_HAVE_GLPK)
#include "linprog/glpk.h"
#elif defined(CONFIG_HAVE_CPLEX)
#include "linprog/cplex.h"
#else
#warning No LP solver available.
#endif


static inline Solution *linprog_solve(
	const LinearProgram& lp,
	unsigned int max_num_vars)
{
	return glpk_solve(lp, max_num_vars);

}

#endif
