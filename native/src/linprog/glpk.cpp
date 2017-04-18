#include <assert.h>
#include <glpk.h>
#include <stdlib.h>

#include <iostream>

#include "cpu_time.h"

#include "linprog/glpk.h"

class GLPKSolution : public Solution
{
private:
	glp_prob *glpk;
	const LinearProgram &linprog;
	const unsigned int num_cols;
	const unsigned int num_rows;
	unsigned int num_coeffs;
	const bool is_mip;

	int simplex_code;
	bool solved;

	void solve(double var_lb, double var_ub);
	void set_objective();
	void set_bounds(double col_lb, double col_ub);
	void set_coefficients();
	void set_column_types();
public:

	GLPKSolution(const LinearProgram &lp, unsigned int max_num_vars,
		     double var_lb = 0.0, double var_ub = 1.0);

	~GLPKSolution();

	void show_error();

	double get_value(unsigned int var) const
	{
		//std::cout << "var: " << var+1 << " value: " << glp_mip_col_val(glpk, var + 1) << std::endl;

		if (is_mip)
			return glp_mip_col_val(glpk, var + 1);
		else
			return glp_get_col_prim(glpk, var + 1);
	}

	bool is_solved() const
	{
		return solved;
	}
};

GLPKSolution::GLPKSolution(const LinearProgram& lp, unsigned int max_num_vars,
			   double var_lb, double var_ub)
	: glpk(glp_create_prob()),
	  linprog(lp),
	  num_cols(max_num_vars),
	  num_rows(lp.get_equalities().size() +
		   lp.get_inequalities().size()),
	  num_coeffs(0),
	  is_mip(lp.has_binary_variables() || lp.has_integer_variables()),
	  solved(false)
{
	if (num_cols){
//		std::cout << std::endl;
//		std::cout << std::endl;
//		std::cout << "constrains: " <<  lp.get_equalities().size() +
//				   lp.get_inequalities().size() << std::endl;
//
//		std::cout << "equal constrains:" << lp.get_equalities().size() << std::endl;
//		foreach(lp.get_equalities(), equ){
//
//			foreach(equ->first->get_terms(), term){
//				std::cout << term->first << term->second << "    ";
//			}
//			std::cout <<  " = " << equ->second << std::endl;
//		}
//
//		std::cout << "not equal constrains:" << lp.get_inequalities().size() << std::endl;
//		foreach(lp.get_inequalities(), equ){
//
//			foreach(equ->first->get_terms(), term){
//				std::cout << term->first << term->second << "    ";
//			}
//			std::cout <<  " <= " << equ->second << std::endl;
//		}




		solve(var_lb, var_ub);
	}
	else
		// Trivial case: no variables.
		// This can happen if a task set does not
		// contain any shared resources.
		solved = true;
}

void GLPKSolution::solve(double var_lb, double var_ub)
{
#if DEBUG_LP_OVERHEADS >= 3
	static DEFINE_CPU_CLOCK(init_costs);
	static DEFINE_CPU_CLOCK(model_costs);
	static DEFINE_CPU_CLOCK(solver_costs);

	init_costs.start();
#endif
	glp_term_out(GLP_OFF);
	glp_set_obj_dir(glpk, GLP_MAX);
	glp_add_cols(glpk, num_cols);
	glp_add_rows(glpk, num_rows);

#if DEBUG_LP_OVERHEADS >= 3
	init_costs.stop();
	model_costs.start();
#endif

	set_objective();
	set_bounds(var_lb, var_ub);
	set_coefficients();
	if (is_mip)
		set_column_types();

#if DEBUG_LP_OVERHEADS >= 3
	model_costs.stop();
	solver_costs.start();
#endif

	if (is_mip)
	{
		glp_iocp glpk_params;

		glp_init_iocp(&glpk_params);

		// presolver is required because otherwise
		// GLPK expects glpk to hold an optimal solution
		// to the relaxed LP.
		glpk_params.presolve = GLP_ON;

		solved = glp_intopt(glpk, &glpk_params) == 0 &&
			 glp_mip_status(glpk) == GLP_OPT;

//		double z = glp_mip_obj_val(glpk);
//
//		std::cout << "total blocking: " << z << std::endl;
//		unsigned long spin = 0;
//		unsigned long arrival = 0;
//		int indexer = 0;
//		foreach(linprog.get_objective()->get_terms(), term)
//		{
//			double coeff     = term->first;
//			unsigned int var = term->second;
//			if(indexer %2 == 0){
//				spin+= coeff * get_value(var);
//			}
//			else{
//				arrival+= coeff * get_value(var);
//			}
//			indexer++;
//		}
//
//		std::cout << "spin delay: " << spin << " arrival blocking : " << arrival << std::endl;
	}
	else
	{
		glp_smcp glpk_params;

		glp_init_smcp(&glpk_params);

		/* Set solver options. The presolver is essential. The other two
		 * options seem to make the solver slightly faster.
		 *
		 * Tested with GLPK 4.43 on wks-50-12.
		 */
		glpk_params.presolve = GLP_ON;
		glpk_params.pricing  = GLP_PT_STD;
		glpk_params.r_test   = GLP_RT_STD;

		simplex_code = glp_simplex(glpk, &glpk_params);
		solved = simplex_code == 0 &&
			glp_get_status(glpk) == GLP_OPT;

	}

#if DEBUG_LP_OVERHEADS >= 3
	solver_costs.stop();

	std::cout << init_costs << std::endl
		  << model_costs << std::endl
		  << solver_costs << std::endl;
#endif
}

GLPKSolution::~GLPKSolution()
{
	glp_delete_prob(glpk);
}

void GLPKSolution::set_objective()
{
	assert(linprog.get_objective()->get_terms().size() <= num_cols);

	foreach (linprog.get_objective()->get_terms(), term)
		glp_set_obj_coef(glpk, term->second + 1, term->first);
}


void GLPKSolution::set_bounds(double col_lb, double col_ub)
{
	unsigned int r = 1;

	foreach(linprog.get_equalities(), equ)
	{
		glp_set_row_bnds(glpk, r++, GLP_FX,
				 equ->second, equ->second);

	        num_coeffs += equ->first->get_terms().size();
	}

	foreach(linprog.get_inequalities(), inequ)
	{
		glp_set_row_bnds(glpk, r++, GLP_UP,
				 0, inequ->second);

		num_coeffs += inequ->first->get_terms().size();
	}

	for (unsigned int c = 1; c <= num_cols; c++)
		glp_set_col_bnds(glpk, c, GLP_DB, col_lb, col_ub);

	foreach(linprog.get_non_default_variable_ranges(), bnds)
	{
		unsigned int c = bnds->variable_id + 1;
		int col_type;
		col_lb = bnds->lower_bound;
		col_ub = bnds->upper_bound;

		if (bnds->has_upper && bnds->has_lower)
			col_type = GLP_DB;
		else if (!bnds->has_upper && !bnds->has_lower)
			col_type = GLP_FR;
		else if (bnds->has_upper)
			col_type = GLP_UP;
		else
			col_type = GLP_LO;

		glp_set_col_bnds(glpk, c, col_type, col_lb, col_ub);
	}
}

void GLPKSolution::set_coefficients()
{
	int *row_idx, *col_idx;
	double *coeff;

	row_idx = new int[1 + num_coeffs];
	col_idx = new int[1 + num_coeffs];
	coeff   = new double[1 + num_coeffs];

	unsigned int r = 1, k = 1;

	foreach(linprog.get_equalities(), equ)
	{
		foreach(equ->first->get_terms(), term)
		{
			assert(k <= num_coeffs);

			row_idx[k] = r;
			col_idx[k] = 1 + term->second;
			coeff[k]   = term->first;

			k += 1;
		}
		r += 1;
	}


	foreach(linprog.get_inequalities(), inequ)
	{
		foreach(inequ->first->get_terms(), term)
		{
			assert(k <= num_coeffs);

			row_idx[k] = r;
			col_idx[k] = 1 + term->second;
			coeff[k]   = term->first;

			k += 1;
		}
		r += 1;
	}

	glp_load_matrix(glpk, num_coeffs, row_idx, col_idx, coeff);

	delete[] row_idx;
	delete[] col_idx;
	delete[] coeff;
}

void GLPKSolution::set_column_types()
{
	unsigned int col_idx;

	foreach(linprog.get_integer_variables(), var_id)
	{
		col_idx = 1 + *var_id;
		// hack: for integer variables, ignore upper bound for now
		glp_set_col_bnds(glpk, col_idx, GLP_LO, 0, 0);
		glp_set_col_kind(glpk, col_idx, GLP_IV);
	}

	foreach(linprog.get_binary_variables(), var_id)
	{
		col_idx = 1 + *var_id;
		glp_set_col_kind(glpk, col_idx, GLP_BV);
	}
}

void GLPKSolution::show_error()
{
	if (!solved)
	{
		std::cerr << "NOT SOLVED => status: "
			<< glp_get_status(glpk)
			<< " (";
		switch (glp_get_status(glpk))
		{
			case GLP_OPT:
				std::cerr << "GLP_OPT";
				break;
			case GLP_FEAS:
				std::cerr << "GLP_FEAS";
				break;
			case GLP_INFEAS:
				std::cerr << "GLP_INFEAS";
				break;
			case GLP_NOFEAS:
				std::cerr << "GLP_NOFEAS";
				break;
			case GLP_UNBND:
				std::cerr << "GLP_UNBND";
				break;
			case GLP_UNDEF:
				std::cerr << "GLP_UNDEF";
				break;
			default:
				std::cerr << "???";
		}
		std::cerr << ") simplex: " << simplex_code << " (";
		switch (glp_get_status(glpk))
		{
			case GLP_EBADB:
				std::cerr << "GLP_EBADB";
				break;
			case GLP_ESING:
				std::cerr << "GLP_ESING";
				break;
			case GLP_ECOND:
				std::cerr << "GLP_ECOND";
				break;
			case GLP_EBOUND:
				std::cerr << "GLP_EBOUND";
				break;
			case GLP_EFAIL:
				std::cerr << "GLP_EFAIL";
				break;
			case GLP_EOBJLL:
				std::cerr << "GLP_EOBJLL";
				break;
			case GLP_EOBJUL:
				std::cerr << "GLP_EOBJUL";
				break;
			case GLP_EITLIM:
				std::cerr << "GLP_EITLIM";
				break;
			case GLP_ENOPFS:
				std::cerr << "GLP_ENOPFS";
				break;
			case GLP_ENODFS:
				std::cerr << "GLP_ENODFS";
				break;
			default:
				std::cerr << "???";
		}

		std::cerr << ")" << std::endl;

	}
}


Solution *glpk_solve(const LinearProgram& lp, unsigned int max_num_vars)
{

	GLPKSolution *sol =  new GLPKSolution(lp, max_num_vars);

	if (sol->is_solved())
		return sol;
	else
	{
		sol->show_error();
		delete sol;
		return NULL;
	}
}
